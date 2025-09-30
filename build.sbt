import org.scalajs.linker.interface.ModuleInitializer
import Dependencies.*
import org.scalajs.jsenv.selenium.SeleniumJSEnv
import org.scalajs.linker.interface.ModuleSplitStyle
import scala.collection.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.3"

lazy val commonJSSettings = Seq(
    fastLinkJS / bundlerImplementation := sbtjsbundler.vite.ViteJSBundler(
        sbtjsbundler.vite.ViteJSBundler.Config()
            .addEnv("NODE_ENV" -> "development")
    ),
    bundlerManagedSources ++= Seq(
        file("laminar-react/js/src/main/javascript"),
        file("laminar-react/js/src/main/styles"),
        file("laminar-react/js/jsbundler"),
    ),
    libraryDependencies ++= Seq(
        // Scala.js DOM API
        "org.scala-js" %%% "scalajs-dom" % "2.8.1",
        // Testing framework
        "com.lihaoyi" %%% "utest" % "0.8.2" % Test,
        // JS implementation of Java time lib needed for test-state
        "io.github.cquiroz" %%% "scala-java-time" % "2.5.0" % Test,
        "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.5.0" % Test,
    ),
    Test / fastLinkJS / scalaJSLinkerConfig ~= { _.withSourceMap(true) },
    // Test configuration with Vite
    Test / jsEnv :=
        //    new SeleniumJSEnv(new org.openqa.selenium.chrome.ChromeOptions().addArguments("--allow-file-access-from-files", "--disable-web-security"), SeleniumJSEnv.Config().withKeepAlive(true))
        new SeleniumJSEnv(new org.openqa.selenium.chrome.ChromeOptions().addArguments("--allow-file-access-from-files", "--disable-web-security")),
)

lazy val root =
    crossProject(JVMPlatform, JSPlatform)
        .in(file("laminar-react"))
        .settings(
            name := "laminar-react",
        )
        .jvmSettings(

        )
        .jsEnablePlugins(JSBundlerPlugin)
        .jsSettings(
            commonJSSettings,
            libraryDependencies ++= laminarDeps.value ++ webAwesomeDeps.value,
            scalaJSModuleInitializers := Seq(ModuleInitializer.mainMethodWithArgs("App", "main")),
        )
