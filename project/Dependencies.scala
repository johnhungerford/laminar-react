import sbt._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {
  val laminarVersion = "17.2.0"
  val webAwesomeVersion = "0.4.0"


  val laminarDeps = Def.setting(Seq(
    "com.raquo" %%% "laminar" % laminarVersion,
  ))

  val webAwesomeDeps = Def.setting(Seq(
    "io.github.nguyenyou" %%% "webawesome-laminar" % webAwesomeVersion,
  ))

}
