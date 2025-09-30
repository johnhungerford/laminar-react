import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.document
import todo.FrameComponent

object App:
    def main(args: Array[String]): Unit =
        val element = FrameComponent()
        render(
            document.querySelector("#app"),
            element,
        )
