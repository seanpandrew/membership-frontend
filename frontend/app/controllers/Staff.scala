package controllers

import javax.inject.Inject

import actions.OAuthActions
import play.api.libs.ws.WSClient
import play.api.mvc.Controller
import services._


class Staff @Inject()(override val wsClient: WSClient) extends Controller with OAuthActions {
  val guLiveEvents = GuardianLiveEventService
  val masterclassEvents = MasterclassEventService

  def eventOverview = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.eventOverview.live(guLiveEvents.events, guLiveEvents.eventsDraft, request.path))
  }

  def eventOverviewMasterclasses = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.eventOverview.masterclasses(masterclassEvents.events, masterclassEvents.eventsDraft, request.path))
  }

  def eventOverviewDetails = GoogleAuthenticatedStaffAction { implicit request =>
    Ok(views.html.eventOverview.details(request.path))
  }
}

