import org.scalajs.linker.interface.ModuleInitializer
import org.scalajs.jsenv.selenium.SeleniumJSEnv
import org.scalajs.linker.interface.ModuleSplitStyle
import scala.collection.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.3"

lazy val root =
    crossProject(JVMPlatform, JSPlatform)
        .in(file("laminar-react"))
        .settings(
            name := "laminar-react",
        )
        .jsEnablePlugins(JSBundlerPlugin)
        .jsSettings(
            fastLinkJS / bundlerImplementation := sbtjsbundler.vite.ViteJSBundler(
                sbtjsbundler.vite.ViteJSBundler.Config()
                    .addEnv("NODE_ENV" -> "development")
            ),
            bundlerManagedSources ++= Seq(
                file("laminar-react/js/jsbundler"),
            ),
            libraryDependencies ++= Seq(
                "org.scala-js" %%% "scalajs-dom" % "2.8.1",
                "com.raquo" %%% "laminar" % "17.2.0",
            ),
            scalaJSModuleInitializers := Seq(ModuleInitializer.mainMethodWithArgs("App", "main")),
        )

Global / onChangedBuildSource := ReloadOnSourceChanges
