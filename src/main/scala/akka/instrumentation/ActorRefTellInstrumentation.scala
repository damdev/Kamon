package akka.instrumentation

import org.aspectj.lang.annotation.{Around, Pointcut, Aspect}
import org.aspectj.lang.ProceedingJoinPoint
import akka.actor.{ActorRef}
import kamon.{Kamon, TraceContext}
import akka.dispatch.Envelope

case class TraceableMessage(traceContext: TraceContext, message: Any)


@Aspect
class ActorRefTellInstrumentation {

  @Pointcut("execution(* akka.actor.ScalaActorRef+.$bang(..)) && !within(akka.pattern.PromiseActorRef) && args(message, sender)")
  def sendingMessageToActorRef(message: Any, sender: ActorRef) = {}

  @Around("sendingMessageToActorRef(message, sender)")
  def around(pjp: ProceedingJoinPoint, message: Any, sender: ActorRef): Unit  = {
    import pjp._

    Kamon.context() match {
      case Some(ctx) => {
        val traceableMessage = TraceableMessage(ctx, message)
        proceed(getArgs.updated(0, traceableMessage))
      }
      case None => proceed
    }
  }
}


@Aspect
class ActorCellInvokeInstrumentation {

  @Pointcut("execution(* akka.actor.ActorCell.invoke(*)) && args(envelope)")
  def invokingActorBehaviourAtActorCell(envelope: Envelope) = {}


  @Around("invokingActorBehaviourAtActorCell(envelope)")
  def around(pjp: ProceedingJoinPoint, envelope: Envelope) = {
    import pjp._

    envelope match {
      case Envelope(TraceableMessage(ctx, msg), sender) => {
        Kamon.set(ctx)

        val originalEnvelope = envelope.copy(message = msg)
        proceed(getArgs.updated(0, originalEnvelope))

        Kamon.clear
      }
      case _ => proceed
    }
  }
}