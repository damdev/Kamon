package kamon.standalone

import akka.actor.Props

import akka.io.IO
import spray.can.Http
import akka.actor.ActorSystem

trait WebApp {

  implicit val system = ActorSystem()

  // create and start our service actor
  val service = system.actorOf(Props[MyServiceActor], "my-service")

  // create a new HttpServer using our handler tell it where to bind to
  IO(Http) ! Http.Bind(service, interface = "localhost", port = 8081)
}

object Boot extends App with WebApp {

}


