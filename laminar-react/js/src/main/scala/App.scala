import article.{Example0Simple, Example1Naive, Example2SplitOne, Example3RouteSignal, Example4SplitArray, Example5CombineSignals, Example6HandleWithSignal}
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.document
import todo.AppComponent
import util.routeSignal
import io.github.nguyenyou.webawesome.laminar.{Card, Divider, Select, UOption}


object App:
    enum State:
        case Example0
        case Example1
        case Example2
        case Example3
        case Example4
        case Example5
        case Example6
        case ToDoApp

        def label = this match {
            case State.Example0 => "Simple reactive element"
            case State.Example1 => "Conditional Rendering: Naive"
            case State.Example2 => "Conditional Rendering: splitOne"
            case State.Example3 => "Conditional Rendering: SignalRouter"
            case State.Example4 => "Conditional Rendering: Array"
            case State.Example5 => "Combining signals"
            case State.Example6 => "Handle events using signals"
            case State.ToDoApp => "To-Do Application"
        }

    private val cases = State.values.toList
    private val stateLookup = cases.map(st => st.toString -> st).toMap

    def main(args: Array[String]): Unit = {
        val state = Var(State.Example0)

        val options = cases.map: example =>
            UOption()(
                example.label,
                value := example.toString,
            )

        val element = div(
            className := "wa-stack",
            className := "wa-align-items-center",
            paddingTop := "30px",
            div(
                width := "650px",
                className := "wa-stack",
                h1("Laminar React examples"),
                Select(
                    _.label := "Choose example",
                )(
                    options,
                    value <-- state.signal.map(_.toString),
                    onChange.mapToValue --> state.writer.contramap((name: String) => stateLookup(name)),
                ),
                Divider()(marginTop := "20px", marginBottom := "10px"),
                Card()(
                    marginTop := "15px",
                    padding := "15px",
                    child <-- state.signal.map(st => h2(st.label)),
                    child <-- state.signal
                        .routeSignal({ case State.Example0 => () })(_ => Example0Simple())
                        .routeSignal({ case State.Example1 => () })(_ => Example1Naive())
                        .routeSignal({ case State.Example2 => () })(_ => Example2SplitOne())
                        .routeSignal({ case State.Example3 => () })(_ => Example3RouteSignal())
                        .routeSignal({ case State.Example4 => () })(_ => Example4SplitArray())
                        .routeSignal({ case State.Example5 => () })(_ => Example5CombineSignals())
                        .routeSignal({ case State.Example6 => () })(_ => Example6HandleWithSignal())
                        .routeSignal({ case State.ToDoApp => () })(_ => AppComponent())
                        .result,
                )
            )

        )

        render(
            document.querySelector("#app"),
            element,
        )
    }
