package part3_clustering

import akka.actor.{Actor, ActorLogging, ActorSystem, Address, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, MemberJoined, MemberRemoved, MemberUp, UnreachableMember}
import com.typesafe.config.ConfigFactory


class ClusterSubscriber extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(
      self,
      initialStateMode =  InitialStateAsEvents,
      classOf[MemberEvent],
      classOf[UnreachableMember]
    )
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {

    case MemberJoined(member) =>
      log.info(s"!!! --->>> Новый актор обнаружен- ${member.address}")
    case MemberUp(member) if member.hasRole("numberCruncher") =>
      log.info(s"!!! --->>> Привет братан - ${member.address}")
    case MemberUp(member) =>
      log.info(s"!!! --->>> Новый актор просоединился- ${member.address}")
    case MemberRemoved(member, previousStatus) =>
      log.info(s"!!! --->>> ${member.address} покинул $previousStatus")
    case UnreachableMember(member) =>
      log.info(s"!!! --->>> ${member.address} потерян")
    case m: MemberEvent =>
      log.info(s"!!! --->>> Другое - ${m}")
//    case MemberJoined(member) =>
//      log.info(s"New member in town: ${member.address}")
//    case MemberUp(member) if member.hasRole("numberCruncher") =>
//      log.info(s"HELLO BROTHER: ${member.address}")
//    case MemberUp(member) =>
//      log.info(s"Let's say welcome to the newest member: ${member.address}")
//    case MemberRemoved(member, previousStatus) =>
//      log.info(s"Poor ${member.address}, it was removed from $previousStatus")
//    case UnreachableMember(member) =>
//      log.info(s"Uh oh, member ${member.address} is unreachable")
//    case m: MemberEvent =>
//      log.info(s"Another member event: $m")
  }
}

object ClusteringBasics extends App {

  def startCluster(ports: List[Int]) =
    ports.foreach { port =>
      val config = ConfigFactory.parseString (
        s"""
          |akka.remote.artery.canonical.port = $port
        """.stripMargin)
        .withFallback(ConfigFactory.load("part3_clustering/clusteringBasics.conf"))

      val system = ActorSystem("AkkaCluster", config)

      system.actorOf(Props[ClusterSubscriber], "ClusterSubscriber")
    }

  startCluster(List(2551,2552,0))
}


object ClusteringBasics_ManualRegistration extends App{

  val system = ActorSystem(
    "AkkaCluster",
    ConfigFactory.load("part3_clustering/clusteringBasics.conf").getConfig("manualRegistration")
  )

  val cluster = Cluster(system)

  def joinExistingCluster = cluster.joinSeedNodes(List(
    Address("akka", "AkkaCluster","localhost",2551),
    Address("akka", "AkkaCluster","localhost",2552)
  ))


  def joinExistingNode = cluster.join(Address("akka", "AkkaCluster","localhost",60718))

  def joinMyself = cluster.join(Address("akka", "AkkaCluster","localhost",2555))

  joinExistingCluster

  system.actorOf(Props[ClusterSubscriber], "ClusterSubscriber")

}
