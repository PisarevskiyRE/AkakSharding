package part2_remoting

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, ActorSystem, Identify, Props}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object RemoteActors extends App {

  val localSystem = ActorSystem("LocalSystem", ConfigFactory.load("part2_remoting/remoteActors.conf"))

  val localSimpleActor = localSystem.actorOf(Props[SimpleActor], "localSympleLocalActor")


  localSimpleActor ! "Привет локальный актор"

  // отправка на удаленный актор
  // метод 1 выбрать актор

  val remoteActorSeletion = localSystem.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteSimpleLocalActor")
  remoteActorSeletion ! "Сообщение на удаленный из локального"

  // 2 из акторрефа
  import localSystem.dispatcher
  implicit val timeout = Timeout(3.seconds)

  val remoteActorRefFuture  = remoteActorSeletion.resolveOne()
  remoteActorRefFuture.onComplete{
    case Success(actroRef) => actroRef ! "отправил в будующее"
    case Failure(ex) => println("не распознан")
  }

  // 3 из индентификатора сообщения
  /*
    - actor resolver запросит выбор актера из локальной системы актеров
    - actor resolver отправит Identify(42) на выбор актера
    - удаленный актер АВТОМАТИЧЕСКИ ответит ActorIdentity(42, actorRef)
    - actor resolver может свободно использовать удаленный actorRef
   */

  class ActorResolver extends Actor with ActorLogging {
    override def preStart(): Unit = {
      val selection  = context.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteSimpleLocalActor")
      selection ! Identify(42)
    }

    override def receive: Receive = {
      case ActorIdentity(42, Some(actorRef)) =>
        actorRef ! "ID получен "
    }
  }


  localSystem.actorOf(Props[ActorResolver], "localActorResolver")

}


object RemoteActors_Remote extends App {
  val remoteSystem = ActorSystem("RemoteSystem", ConfigFactory.load("part2_remoting/remoteActors.conf").getConfig("remoteSystem"))
  val remoteSimpleActor = remoteSystem.actorOf(Props[SimpleActor], "remoteSimpleLocalActor")

  remoteSimpleActor ! "Привет удаленный актор"
}