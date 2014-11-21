package controllers

import actions.Functions._
import actions._
import com.gu.membership.salesforce.{ScalaforceError, Tier}
import com.netaporter.uri.dsl._
import configuration.{Config, CopyConfig}
import forms.MemberForm.{JoinForm, friendJoinForm, paidMemberJoinForm}
import model.Eventbrite.{EBCode, RichEvent}
import model.StripeSerializer._
import model._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Controller, Request, Result}
import services._

import scala.concurrent.Future

trait Joiner extends Controller {

  val memberService: MemberService
  val eventService: EventbriteService

  def getEbIFrameDetail(request: AnyMemberTierRequest[_]): Future[Option[(String, Int)]] = {
    def getEbEventFromSession(request: Request[_]): Option[RichEvent] =
      PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(eventService.getEvent)

    def detailsFor(event: RichEvent, discountOpt: Option[EBCode]): (String, Int) = {
      val url = (Config.eventbriteApiIframeUrl ? ("eid" -> event.id) & ("discount" -> discountOpt.map(_.code))).toString
      (url, event.ticket_classes.length)
    }

    Future.sequence {
      (for (event <- getEbEventFromSession(request)) yield {
        for (discountOpt <- memberService.createDiscountForMember(request.member, event)) yield detailsFor(event, discountOpt)
      }).toSeq
    }.map(_.headOption)
  }

  def tierList = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleJoin,
      request.path,
      Some(CopyConfig.copyDescriptionJoin)
    )
    Ok(views.html.joiner.tierList(pageInfo))
  }

  def staff = GoogleAuthenticatedStaffNonMemberAction { implicit request =>
    val error = request.flash.get("error")
    Ok(views.html.joiner.staff(error))
  }

  def enterDetails(tier: Tier.Tier) = AuthenticatedNonMemberAction.async { implicit request =>
    val identityRequest = IdentityRequest(request)
    for {
      (privateFields, marketingChoices, passwordExists) <- identityDetails(request.user, identityRequest)
    } yield {

      tier match {
        case Tier.Friend => Ok(views.html.joiner.detail.addressForm(privateFields, marketingChoices, passwordExists))
        case paidTier =>
          val pageInfo = PageInfo.default.copy(stripePublicKey = Some(request.touchpointBackend.stripeService.publicKey))
          Ok(views.html.joiner.payment.paymentForm(paidTier, privateFields, marketingChoices, passwordExists, pageInfo))
      }

    }
  }

  def enterStaffDetails = GoogleAndIdentityAuthenticatedStaffNonMemberAction.async { implicit request =>
    val identityRequest = IdentityRequest(request)
    for {
      (privateFields, marketingChoices, passwordExists) <- identityDetails(request.identityUser, identityRequest)
    } yield {
      Ok(views.html.joiner.detail.addressForm(privateFields, marketingChoices, passwordExists))
    }
  }

  private def identityDetails(user: com.gu.identity.model.User, identityRequest: IdentityRequest) = {
    for {
      userOpt <- IdentityService.getFullUserDetails(user, identityRequest)
      privateFields = userOpt.fold(PrivateFields())(_.privateFields)
      marketingChoices = userOpt.fold(StatusFields())(_.statusFields)
      passwordExists <- IdentityService.doesUserPasswordExist(identityRequest)
    } yield (privateFields, marketingChoices, passwordExists)
  }

  def joinFriend() = AuthenticatedNonMemberAction.async { implicit request =>
    friendJoinForm.bindFromRequest.fold(_ => Future.successful(BadRequest),
      makeMember { Redirect(routes.Joiner.thankyouFriend()) } )
  }

  def joinPaid(tier: Tier.Tier) = AuthenticatedNonMemberAction.async { implicit request =>
    paidMemberJoinForm.bindFromRequest.fold(_ => Future.successful(BadRequest),
      makeMember { Ok(Json.obj("redirect" -> routes.Joiner.thankyouPaid(tier).url)) } )
  }

  private def makeMember(result: Result)(formData: JoinForm)(implicit request: AuthRequest[_]) = {
    MemberService.createMember(request.user, formData, IdentityRequest(request))
      .map { _ => result }
      .recover {
        case error: Stripe.Error => Forbidden(Json.toJson(error))
        case error: Zuora.ResultError => Forbidden
        case error: ScalaforceError => Forbidden
      }
  }

  def thankyouFriend() = MemberAction.async { implicit request =>
    for {
      subscriptionDetails <- request.touchpointBackend.subscriptionService.getCurrentSubscriptionDetails(request.member.salesforceAccountId)
      eventbriteFrameDetail <- getEbIFrameDetail(request)
    } yield {
      val event = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(eventService.getEvent)
      Ok(views.html.joiner.thankyou.friend(subscriptionDetails, eventbriteFrameDetail, request.member.firstName.getOrElse(""), request.user.primaryEmailAddress, event))
    }
  }

  def thankyouPaid(tier: Tier.Tier, upgrade: Boolean = false) = PaidMemberAction.async { implicit request =>
    for {
      customer <- request.touchpointBackend.stripeService.Customer.read(request.member.stripeCustomerId)
      subscriptionDetails <- request.touchpointBackend.subscriptionService.getCurrentSubscriptionDetails(request.member.salesforceAccountId)
      eventbriteFrameDetail <- getEbIFrameDetail(request)
    } yield {
      val event = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(eventService.getEvent)
      Ok(views.html.joiner.thankyou.paid(customer.card, subscriptionDetails, eventbriteFrameDetail, tier, request.member.firstName.getOrElse(""), request.user.primaryEmailAddress, event, upgrade))
    }
  }
}

object Joiner extends Joiner {
  val memberService = MemberService
  val eventService = GuardianLiveEventService
}
