package services

import akka.agent.Agent
import configuration.Config
import model.EventPortfolio
import model.Eventbrite._
import model.EventbriteDeserializer._
import monitoring.EventbriteMetrics
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.Reads
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait EventbriteService extends utils.Http[EBObject, EBError] {
  val apiToken: String

  val apiUrl = Config.eventbriteApiUrl
  val statusMetrics = EventbriteMetrics

  def authenticateRequest(req: WSRequestHolder): WSRequestHolder = req.withQueryString("token" -> apiToken)

  val apiEventListUrl: String

  def events: Seq[EBEvent]

  lazy val allEvents = Agent[Seq[EBEvent]](Seq.empty)
  lazy val priorityOrderedEventIds = Agent[Seq[String]](Seq.empty)

  private def getPaginated[T](url: String)(implicit reads: Reads[EBResponse[T]]): Future[Seq[T]] = {
    val enumerator = Enumerator.unfoldM(Option(1)) {
      _.map { nextPage =>
        for {
          response <- get[EBResponse[T]](url, "page" -> nextPage.toString)
        } yield Some((response.pagination.nextPageOpt, response.data))
      }.getOrElse(Future.successful(None))
    }

    enumerator(Iteratee.consume()).flatMap(_.run)
  }

  private def getAllEvents: Future[Seq[EBEvent]] = getPaginated[EBEvent](apiEventListUrl)

  private def getPriorityEventIds(): Future[Seq[String]] =  for {
    ordering <- WS.url(Config.eventOrderingJsonUrl).get()
  } yield (ordering.json \ "order").as[Seq[String]]

  def getEventPortfolio: EventPortfolio = {
    val priorityIds = priorityOrderedEventIds.get()
    val (priorityEvents, normal) = getPortfolioEvents.partition(e => priorityIds.contains(e.id))

    EventPortfolio(priorityEvents.sortBy(e => priorityIds.indexOf(e.id)), normal)
  }

  def getPortfolioEvents: Seq[EBEvent] = events.filter(_.getStatus.isInstanceOf[DisplayableEvent])

  /**
   * scuzzy implementation to enable basic 'filtering by tag' - in this case, just matching the event name.
   */
  def getEventsTagged(tag: String) = getPortfolioEvents.filter(_.name.text.toLowerCase.contains(tag))

  def getEvent(id: String): Option[EBEvent] = allEvents.get().find(_.id == id)

  def createOrGetAccessCode(event: EBEvent, code: String, ticketClasses: Seq[EBTicketClass]): Future[EBAccessCode] = {
    val uri = s"events/${event.id}/access_codes"

    for {
      discounts <- getPaginated[EBAccessCode](uri)
      discount <- discounts.find(_.code == code).fold {
        post[EBAccessCode](uri, Map(
          "access_code.code" -> Seq(code),
          "access_code.quantity_available" -> Seq("2"),
          "access_code.ticket_ids" -> Seq(ticketClasses.head.id) // TODO: support multiple ticket classes when Eventbrite fix their API
        ))
      }(Future.successful)
    } yield discount
  }

  def createOrGetDiscount(eventId: String, code: String): Future[EBDiscount] = {
    val uri = s"events/$eventId/discounts"

    for {
      discounts <- getPaginated[EBDiscount](uri)
      discount <- discounts.find(_.code == code).map(Future.successful).getOrElse {
        post[EBDiscount](uri, Map(
          "discount.code" -> Seq(code),
          "discount.percent_off" -> Seq("20"),
          "discount.quantity_available" -> Seq("2")
        ))
      }
    } yield discount
  }

  def getOrder(id: String): Future[EBOrder] = get[EBOrder](s"orders/$id")
}

object EventbriteService extends EventbriteService {
  val apiToken = Config.eventbriteApiToken
  val apiEventListUrl = Config.eventbriteApiEventListUrl

  val refreshTimeAllEvents = new FiniteDuration(Config.eventbriteRefreshTimeForAllEvents, SECONDS)
  val refreshTimePriorityEvents = new FiniteDuration(Config.eventbriteRefreshTimeForPriorityEvents, SECONDS)

  def events: Seq[EBEvent] = allEvents.get()

  import play.api.Play.current

  def start() {
    def scheduleAgentRefresh[T](agent: Agent[T], refresher: => Future[T], intervalPeriod: FiniteDuration) = {
      Akka.system.scheduler.schedule(1.second, intervalPeriod) {
        agent.sendOff(_ => Await.result(refresher, 15.seconds))
      }
    }
    Logger.info("Starting EventbriteService background tasks")
    scheduleAgentRefresh(allEvents, getAllEvents, refreshTimeAllEvents)
    scheduleAgentRefresh(priorityOrderedEventIds, getPriorityEventIds, refreshTimePriorityEvents)
  }
}
