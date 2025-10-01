import article.{Example1Naive, Example2SplitOne, Example3RouteSignal, Example4SplitArray}
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.document
import todo.AppComponent
import util.routeSignal

object App:
    enum State:
        case Example1
        case Example2
        case Example3
        case Example4
        case ToDoApp

        def label = this match {
            case State.Example1 => "Conditional Rendering: Naive"
            case State.Example2 => "Conditional Rendering: splitOne"
            case State.Example3 => "Conditional Rendering: SignalRouter"
            case State.Example4 => "Conditional Rendering: Array"
            case State.ToDoApp => "To-Do Application"
        }

    def main(args: Array[String]): Unit = {
        val state = Var(State.Example1)

        val cases = State.values.toList
        val stateLookup = cases.map(st => st.toString -> st).toMap

        val options = cases.map: example =>
            option(
                example.label,
                value := example.toString,
            )

        val element = div(
            h1("Laminar React examples"),
            p("Choose example"),
            select(
                options,
                value <-- state.signal.map(_.toString),
                onChange.mapToValue --> state.writer.contramap((name: String) => {
                    val state = stateLookup(name)
                    println(state)
                    state
                }),
            ),
            child <-- state.signal.map(st => h2(st.label)),
            child <-- state.signal
                .routeSignal({ case State.Example1 => () })(_ => Example1Naive.element)
                .routeSignal({ case State.Example2 => () })(_ => Example2SplitOne.element)
                .routeSignal({ case State.Example3 => () })(_ => Example3RouteSignal.element)
                .routeSignal({ case State.Example4 => () })(_ => Example4SplitArray.element)
                .routeSignal({ case State.ToDoApp => () })(_ => AppComponent())
                .result,
            padding := "100px",
        )

        render(
            document.querySelector("#app"),
            element,
        )
    }
