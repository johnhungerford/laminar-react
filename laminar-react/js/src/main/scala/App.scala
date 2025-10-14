import article.{Example1, Example10, Example2, Example3, Example4, Example5, Example6, Example7, Example8, Example9, StatelessInput}
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.document
import todo.AppComponent
import common.styles.{Flex, card, customOption, customSelect}


object App:
    enum State:
        case Example1
        case Example2
        case Example3
        case Example4
        case Example5
        case Example6
        case Example7
        case Example8
        case Example9
        case Example10
        case ToDoApp

        def label: String = this match
            case State.Example1 => "Example 1: Stateless with input"
            case State.Example2 => "Example 2: Stateless with input and output"
            case State.Example3 => "Example 3: Render array"
            case State.Example4 => "Example 4: Render array incorrectly"
            case State.Example5 => "Example 5: Simple conditional rendering"
            case State.Example6 => "Example 6: Simple conditional rendering (incorrect)"
            case State.Example7 => "Example 7: Complex conditional rendering"
            case State.Example8 => "Example 8: Complex conditional rendering (incorrect)"
            case State.Example9 => "Example 9: Simple state management"
            case State.Example10 => "Example 10: Complex state management"
            case State.ToDoApp => "Example Application: To-Do Lists"

    private val cases = State.values.toList
    private val stateLookup = cases.map(st => st.toString -> st).toMap

    def main(args: Array[String]): Unit = {
        val state = Var(State.Example1)

        val options = cases.map: example =>
            customOption(
                example.label,
                value := example.toString,
            )

        val element = Flex.column(
            paddingTop := "30px",
            alignItems.center,
            Flex.column(
                width := "700px",
                h1("Laminar React examples"),
                Flex.row(
                    "Choose example",
                    customSelect(
                        fontSize.inherit,
                        options,
                        value <-- state.signal.map(_.toString),
                        onChange.mapToValue
                            .map(stateLookup.get)
                            .collect({ case Some(v) => v }) --> state.writer,
                    ),
                ),
                card(
                    marginTop := "15px",
                    padding := "25px",
                    child <-- state.signal.map(st => h1(st.label)),
                    child <-- state.signal.splitMatchOne
                        .handleType[State.Example1.type]((_, _) => Example1())
                        .handleType[State.Example2.type]((_, _) => Example2())
                        .handleType[State.Example3.type]((_, _) => Example3())
                        .handleType[State.Example4.type]((_, _) => Example4())
                        .handleType[State.Example5.type]((_, _) => Example5())
                        .handleType[State.Example6.type]((_, _) => Example6())
                        .handleType[State.Example7.type]((_, _) => Example7())
                        .handleType[State.Example8.type]((_, _) => Example8())
                        .handleType[State.Example9.type]((_, _) => Example9())
                        .handleType[State.Example10.type]((_, _) => Example10())
                        .handleType[State.ToDoApp.type]((_, _) => AppComponent())
                        .toSignal,
                )
            )
        )

        render(
            document.querySelector("#app"),
            element,
        )
    }
