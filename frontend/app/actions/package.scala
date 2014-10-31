import com.gu.identity
import com.gu.membership.salesforce.{Member, PaidMember, Tier}
import com.gu.membership.util.Timing
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.WrappedRequest
import services._
import utils.TestUsers

import scala.concurrent.{ExecutionContext, Future}

package object actions {
  type AuthRequest[A] = AuthenticatedRequest[A, identity.model.User]

  implicit class RichAuthRequest[A](req: AuthRequest[A]) {
    def forMemberOpt[A, T](f: Option[Member] => T)(implicit executor: ExecutionContext): Future[T] =
      Timing.record(MemberAuthenticationMetrics, s"${req.method} ${req.path}") {
        for {
          memberOpt <- touchpointBackend.memberRepository.get(req.user.id).map(_.filter(_.tier > Tier.None))
        } yield f(memberOpt)
      }
    }

  implicit class RichUser(user: identity.model.User) {
    lazy val isTestUser: Boolean = TestUsers.validate(user)
  }

  case class MemberRequest[A, +M <: Member](val member: M, request: AuthRequest[A]) extends WrappedRequest[A](request) {
    val user = request.user
  }

  type AnyMemberTierRequest[A] = MemberRequest[A, Member]

  type PaidMemberRequest[A] = MemberRequest[A, PaidMember]
}
