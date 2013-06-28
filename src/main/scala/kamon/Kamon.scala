package kamon

import akka.actor.{Props, ActorSystem}
import standalone.MyServiceActor
import akka.io.IO
import spray.can.Http

object Kamon {

  val ctx = new ThreadLocal[Option[TraceContext]] {
    override def initialValue() = None
  }

  implicit lazy val actorSystem = ActorSystem("kamon")


  def context() = ctx.get()
  def clear = ctx.remove()
  def set(traceContext: TraceContext) = ctx.set(Some(traceContext))

  def startKamonDashboard() ={
    // create and start our service actor
    val system = ActorSystem()

    val service = system.actorOf(Props[MyServiceActor], "my-service")

    // create a new HttpServer using our handler tell it where to bind to
    IO(Http) ! Http.Bind(service, interface = "localhost", port = 8081)
  }

  def start = {
    startKamonDashboard()
    set(newTraceContext)
  }
  def stop = ctx.get match {
    case Some(context) => context.close
    case None =>
  }

  def newTraceContext(): TraceContext = TraceContext()

  val publisher = actorSystem.actorOf(Props[TransactionPublisher])

  def publish(tx: FullTransaction) = publisher ! tx
}
