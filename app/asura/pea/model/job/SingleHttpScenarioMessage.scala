package asura.pea.model.job

import asura.pea.common.util.StringUtils
import asura.pea.model.params._
import asura.pea.model.{Injection, LoadMessage, LoadTypes, SingleRequest}

case class SingleHttpScenarioMessage(
                                      var name: String,
                                      var request: SingleRequest,
                                      var injections: Seq[Injection],
                                      var report: Boolean = true,
                                      var simulationId: String = null,
                                      var start: Long = 0L,
                                      var feeder: FeederParam = null,
                                      var loop: LoopParam = null,
                                      var maxDuration: DurationParam = null,
                                      var assertions: HttpAssertionParam = null,
                                      var throttle: ThrottleParam = null,
                                    ) extends LoadMessage {

  val `type`: String = LoadTypes.SINGLE

  def isValid(): Exception = {
    if (null == request || StringUtils.isEmpty(request.url)) {
      new RuntimeException("Empty request")
    } else if (null == injections || injections.isEmpty) {
      new RuntimeException("Empty injections")
    } else {
      null
    }
  }
}
