package kamon.standalone

import akka.actor.Actor
import spray.routing._
import directives.LogEntry
import spray.http._
import MediaTypes._
import akka.event.Logging
import java.io.File
import shapeless._


// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with MyService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}

// this trait defines our service behavior independently from the service actor
trait MyService extends HttpService with StaticResources with TwirlPages {

  def showPath(req: HttpRequest) = LogEntry(s"Method = ${req.method}, Path = ${req.uri}", Logging.InfoLevel)

  val myRoute =
    logRequest(showPath _) {
      staticResources ~ twirlPages
    }
}

// Trait for serving static resources
// Sends 404 for 'favicon.icon' requests and serves static resources in 'bootstrap' folder.
trait StaticResources extends HttpService {

  val staticResources =
    get {
      path("favicon.ico") {
        complete(StatusCodes.NotFound)
      } ~
      path(Rest) { path =>
        getFromResource(s"bootstrap/$path")
      } ~
      path(Rest) { path =>
        getFromResource(s"highcharts/$path")
      }
    }
}

// Trait for serving some dynamic Twirl pages
trait TwirlPages extends HttpService {

  val twirlPages =
    get {
      path("index") {
        respondWithMediaType(`text/html`) {
          complete(html.index("KAMON", "Kamon").toString)
        }
      }
    }
}

