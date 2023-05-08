package part1_recap

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, PoisonPill, Props, Stash, SupervisorStrategy}
import akka.util.Timeout

object AkkaRecap extends App {


  class SimpleActor extends Actor with ActorLogging with Stash{
    override def receive: Receive = {
      case "createChild" =>
        val childActor = context.actorOf(Props[SimpleActor], "Потомок")
        childActor ! "Привет"
      case "stashThis" =>
        stash()
      case "change handler NOW" =>
        unstashAll()
        context.become(anotherHandler)
      case "change" => context.become(anotherHandler)
      case message => println(s"Я получил сообщение -> $message")
    }

    def anotherHandler: Receive = {
      case message => println(s"Сообщение из друго обработчика: $message")
    }

    override def preStart(): Unit = {
      log.info("Я запускаюсь")
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(){
      case _: RuntimeException => Restart
      case _ => Stop
    }
  }

  // акторная подсистема
  val system = ActorSystem("AkkaRecap")

  // инстанс актора
  val actor = system.actorOf(Props[SimpleActor], "simpleActor")

  // отправка сообщения
  actor ! "Привет"

  /*
    - сообщения отправляются асинхронно
    - большое количество(миллионы) акторов могут быть в всего нескольких потоках
    - каждое сообщение обрабатывается атомарно
    - и из-за этого не нужны блокировки
   */

  // изменение поведения актора + хранение
  // акторы могут создавать другие акторы
  // главные акторы: /system, /user, / = root guardian

  // акторы имеют определенный жизненный цикл: started, stopped, suspended, resumed, restarted

  // остановка акторов - context.stop
  actor ! PoisonPill

  // logging
  // supervision

  // для конфигурации: dispatcher, routes, mailboxes

  // schedulers
  import scala.concurrent.duration._
  import system.dispatcher
  system.scheduler.scheduleOnce(2.seconds){
    actor ! "Задержка"
  }

  // akka ask
  import akka.pattern.ask
  implicit val timeout = Timeout(3.seconds)

  val future = actor ? "вопрос"

  // pipe пересылка результата в другой актор
  import akka.pattern.pipe
  val anotherActor = system.actorOf(Props[SimpleActor], "anotherSimpleActor")

  future.mapTo[String].pipeTo(anotherActor)


}
