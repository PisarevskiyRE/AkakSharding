package part2_remoting

import akka.actor.{Actor, ActorLogging, ActorSystem, Address, AddressFromURIString, Deploy, PoisonPill, Props, Terminated}
import akka.remote.RemoteScope
import akka.routing.FromConfig
import com.typesafe.config.ConfigFactory

object DeployingActorsRemotely_LocalApp extends App {
  val system = ActorSystem("LocalActorSystem", ConfigFactory.load("part2_remoting/deployingActorsRemotely.conf").getConfig("localApp"))

  val simpleActor = system.actorOf(Props[SimpleActor], "remoteActor") // /user/remoteActor
  simpleActor ! "hello, remote actor!"

  println(simpleActor)

  val remoteSystemAddress: Address = AddressFromURIString("akka://RemoteActorSystem@localhost:2552")
  val remotelyDeployedActor = system.actorOf(
    Props[SimpleActor].withDeploy(
      Deploy(scope = RemoteScope(remoteSystemAddress))
    )
  )

}

object DeployingActorsRemotely_RemoteApp extends App {
  val system = ActorSystem("RemoteActorSystem", ConfigFactory.load("part2_remoting/deployingActorsRemotely.conf").getConfig("remoteApp"))
}