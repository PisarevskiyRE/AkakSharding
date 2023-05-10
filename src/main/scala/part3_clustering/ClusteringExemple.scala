package part3_clustering

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Address, Props, ReceiveTimeout}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import akka.util.Timeout

import scala.concurrent.duration._
import akka.pattern.pipe
import com.typesafe.config.{Config, ConfigFactory}

object ClusteringExampleDomain {

  case class ProcessFile(filename: String)
  case class ProcessLine(line: String)
  case class ProcessLineResult(count: Int)
}


class Master extends Actor with ActorLogging {

  import ClusteringExampleDomain._

  import context.dispatcher

  implicit val timeout = Timeout(3.seconds)

  val cluster = Cluster(context.system)

  var workers: Map[Address, ActorRef] = Map()
  var pendingRemoval: Map[Address, ActorRef] = Map()

  override def preStart(): Unit = {
    cluster.subscribe(
      self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent],
      classOf[UnreachableMember]
    )
  }


  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive = handleClusterEvents



  def handleClusterEvents: Receive = {
    case MemberUp(member) if member.hasRole("worker") =>
      log.info(s"!!! --->>> Новый актор просоединился- ${member.address}")

      if (pendingRemoval.contains(member.address)){
        pendingRemoval = pendingRemoval - member.address
      } else {
        val workerSelection = context.actorSelection(s"${member.address}/user/worker")
        workerSelection.resolveOne().map(ref => (member.address, ref)).pipeTo(self)
      }


    case UnreachableMember(member) if member.hasRole("worker") =>
      log.info(s"!!! --->>> ${member.address} потерян")

      val workerOption = workers.get(member.address)
      workerOption.foreach { ref =>
        pendingRemoval = pendingRemoval + (member.address -> ref)
      }

    case MemberRemoved(member, previousStatus) =>
      log.info(s"!!! --->>> ${member.address} покинул после $previousStatus")

      workers = workers - member.address

    case m: MemberEvent =>
      log.info(s"!!! --->>> Другое - ${m}")
  }
}
