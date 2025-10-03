package todo

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.HTMLInputElement
import todo.model.GlobalEvent
import util.routeSignal
import util.StateContainer
import io.github.nguyenyou.webawesome.laminar.{Divider, Select, UOption, Card, Button, Input}


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

    def apply(propsSignal: Signal[Props])(using globalState: GlobalStore): HtmlElement =
        val localState = StateContainer[State, Event](
            State.Initial,
            (state, event) => state.reduce(event),
        )

        // Certain of the local events (State.Add) should trigger an update to global state
        val globalEvents = localState.events.withCurrentValueOf(localState.state, propsSignal).collect:
            case (Event.Add, State.Adding(name), Props(existingNames)) if !existingNames.contains(name.strip()) =>
                GlobalEvent.NewList(name.strip())

        val adder = localState.state.combineWith(propsSignal)
            .routeSignal({ case (State.Initial, _) => () }) { _ =>
                Button(_.appearance.filled)(
                    "New list",
                    onClick.mapTo(Event.StartAdding) --> localState.input,
                )
            }
            .routeSignal({ case (st: State.Adding, props) => (st, props) }) { signal =>
                val addingSignal = signal.map(_._1)

                val addDisabled: Signal[Boolean] = signal.map:
                    case (State.Adding(nameText), Props(existingNames)) =>
                        val stripped = nameText.strip()
                        stripped.isEmpty || existingNames.contains(stripped)

                Card()(
                    div(
                        className := "wa-stack",
                        h4("New list"),
                        Input()(
                            value <-- addingSignal.map(_.nameText),
                            onInput.mapToValue.map(Event.SetNameText(_)) --> localState.input,
                        ),
                        div(
                            className := "wa-cluster",
                            Button(_.appearance.filled)("Add", disabled <-- addDisabled, onClick.mapTo(Event.Add) --> localState.input),
                            Button(_.appearance.filled)("Cancel", onClick.mapTo(Event.StopAdding) --> localState.input),
                        )
                    )
                )
            }
            .result

        div(
            child <--adder,
            localState.bind,
            globalEvents --> globalState.input,
        )
