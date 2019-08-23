package asura.pea.api

import java.io.File

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.Materializer
import asura.common.model.{ApiRes, ApiResError}
import asura.pea.PeaConfig
import asura.pea.PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.pea.actor.PeaReporterActor.{RunSimulationJob, SingleHttpScenarioJob}
import asura.pea.model.{LoadJob, PeaMember}
import asura.play.api.BaseApi
import asura.play.api.BaseApi.OkApiRes
import controllers.Assets
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.http.HttpErrorHandler
import play.api.mvc._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeApi @Inject()(
                         implicit val system: ActorSystem,
                         implicit val exec: ExecutionContext,
                         implicit val mat: Materializer,
                         val controllerComponents: SecurityComponents,
                         val assets: Assets,
                         val errorHandler: HttpErrorHandler,
                       ) extends BaseApi with CommonFunctions {

  def index() = assets.at("index.html")

  def asset(resource: String): Action[AnyContent] = if (resource.startsWith("api")) {
    Action.async(r => errorHandler.onClientError(r, NOT_FOUND, "Not found"))
  } else {
    if (resource.contains(".")) assets.at(resource) else index
  }

  def report(filePath: String) = Action {
    val file = new File(s"${PeaConfig.resultsFolder}/${filePath}")
    if (file.isDirectory) {
      TemporaryRedirect(s"/report/${filePath}/index.html")
    } else {
      if (file.exists()) {
        if (file.getAbsolutePath.startsWith(PeaConfig.resultsFolder)) {
          Ok.sendFile(file, true)
        } else {
          OkApiRes(ApiResError(s"Blocking access to this file: ${file.getAbsolutePath}"))
        }
      } else {
        OkApiRes(ApiResError(s"File is not there: ${file.getAbsolutePath}"))
      }
    }
  }

  def reports() = Action {
    val file = new File(PeaConfig.resultsFolder)
    val folders = file.listFiles().filter(_.isDirectory).map(_.getName)
    OkApiRes(ApiRes(data = folders))
  }

  def jobs() = Action.async { implicit req =>
    val children = PeaConfig.zkClient.getChildren.forPath(s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_JOBS}")
    Future.successful(children).toOkResult
  }

  def workers() = Action.async { implicit req =>
    val children = PeaConfig.zkClient.getChildren.forPath(s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_WORKERS}")
    Future.successful(children.asScala.map(PeaMember(_)).filter(m => null != m)).toOkResult
  }

  def reporters() = Action.async { implicit req =>
    val children = PeaConfig.zkClient.getChildren.forPath(s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_REPORTERS}")
    Future.successful(children.asScala.map(PeaMember(_)).filter(m => null != m)).toOkResult
  }

  def single() = Action(parse.byteString).async { implicit req =>
    checkReporterEnable {
      val message = req.bodyAs(classOf[SingleHttpScenarioJob])
      loadJob(message)
    }
  }

  def runSimulation() = Action(parse.byteString).async { implicit req =>
    checkReporterEnable {
      val message = req.bodyAs(classOf[RunSimulationJob])
      loadJob(message)
    }
  }

  private def loadJob(message: LoadJob): Future[Result] = {
    val workers = message.workers
    val request = message.request
    if (null == workers || workers.isEmpty) {
      FutureErrorResult("Empty workers")
    } else {
      if (null != request) {
        val exception = request.isValid()
        if (null != exception) {
          Future.failed(exception)
        } else {
          (PeaConfig.reporterActor ? message).toOkResult
        }
      } else {
        FutureErrorResult("Empty request")
      }
    }
  }

  private def checkReporterEnable(func: => Future[Result]): Future[Result] = {
    if (PeaConfig.enableReporter) {
      func
    } else {
      FutureErrorResult("Role reporter is disabled")
    }
  }
}
