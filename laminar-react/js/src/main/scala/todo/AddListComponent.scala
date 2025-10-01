package todo

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.HTMLInputElement
import todo.model.GlobalEvent
import util.routeSignal
import util.StateContainer


object AddListComponent:
    final case class Props(existingNames: Set[String])

    enum Event:
        case StartAdding
        case StopAdding
        case SetNameText(value: String)
        case Add

    enum State:
        case Initial
        case Adding(nameText: String)

        def reduce(event: Event): State = event match
            case Event.StartAdding => Adding("")
            case Event.StopAdding => Initial
            case Event.SetNameText(value) => Adding(value)
            case Event.Add => Initial

    def apply(propsSignal: Signal[Props]): HtmlElement =
        val stateContainer = StateContainer[State, Event](
            State.Initial,
            (state, event) => state.reduce(event),
        )

        // Certain of the local events (State.Add) should trigger an update to global state
        val globalEvents = stateContainer.events.withCurrentValueOf(stateContainer.state, propsSignal).collect:
            case (Event.Add, State.Adding(name), Props(existingNames)) if !existingNames.contains(name.strip()) =>
                GlobalEvent.NewList(name.strip())

        val adder = stateContainer.state.combineWith(propsSignal)
            .routeSignal({ case (State.Initial, _) => () }) { _ =>
                button(
                    "Add list",
                    onClick.mapTo(Event.StartAdding) --> stateContainer.input,
                )
            }
            .routeSignal({ case (st: State.Adding, props) => (st, props) }) { signal =>
                val addingSignal = signal.map(_._1)

                val addDisabled = signal.map:
                    case (State.Adding(nameText), Props(existingNames)) => existingNames.contains(nameText.strip())

                div(
                    "New list:",
                    input(
                        controlled(
                            value <-- addingSignal.map(_.nameText),
                            onInput.mapToValue.map(Event.SetNameText(_)) --> stateContainer.input,
                        )
                    ),
                    button("Add", disabled <-- addDisabled, onClick.mapTo(Event.Add) --> stateContainer.input),
                    button("Cancel", onClick.mapTo(Event.StopAdding) --> stateContainer.input),
                )
            }
            .result

        div(
            child <--adder,
            stateContainer.bind,
            globalEvents --> globalState.input,
        )
