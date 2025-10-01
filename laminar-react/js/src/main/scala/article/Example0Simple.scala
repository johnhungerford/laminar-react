package article

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.{HTMLDivElement, document}

object Example0Simple:
    def apply(): HtmlElement =
        val textState: Var[String] = Var("Name") // Reactive "state" for the text to display

        div(
            h3("Example"),
            text <-- textState.signal, // The element text will be updated with the state
            input(
                value <-- textState.signal, // The input text will be set by the state
                onInput.mapToValue --> textState.writer, // Updates to the text will propagate to the state
            ),
            padding := "20px", // Properties can also be set with static values
        )
