package todo

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.{HTMLDivElement, HTMLInputElement, HTMLSelectElement}
import todo.model.{GlobalEvent, GlobalState, ToDo, ToDoList, ToDoListState}
import todo.util.StateContainer
import todo.util.routeSignal


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

        def reduce(event: Event): State = event match {
            case Event.StartAdding => Adding("")
            case Event.StopAdding => Initial
            case Event.SetNameText(value) => Adding(value)
            case Event.Add => Initial
        }

    def apply(props: Signal[Props]): HtmlElement =
        val stateContainer = StateContainer[State, Event](
            State.Initial,
            (state, event) => state.reduce(event),
        )

        val globalEvents = stateContainer.events.withCurrentValueOf(stateContainer.state, props).collect:
            case (Event.Add, State.Adding(name), Props(existingNames)) if !existingNames.contains(name.strip()) =>
                GlobalEvent.NewList(name.strip())

        val adder = stateContainer.state.combineWith(props)
            .routeSignal({ case (State.Initial, _) => () })(
                _ => button("Add list", onClick.mapTo(Event.StartAdding) --> stateContainer.input)
            )
            .routeSignal({ case tup @ (_: State.Adding, props) => tup }) { signal =>
                val addingSignal = signal.map:
                    case (addingState: State.Adding, props) => Some((addingState, props))
                    case _ => None

                div(
                    "New list:",
                    input(
                        controlled(
                            value <-- addingSignal.map(_.map(_._1.nameText).getOrElse("")),
                            onInput.map(v => Event.SetNameText(v.currentTarget.asInstanceOf[HTMLInputElement].value)) --> stateContainer.input,
                        )
                    ),
                    button("Add", onClick.mapTo(Event.Add) --> stateContainer.input),
                    button("Cancel", onClick.mapTo(Event.StopAdding) --> stateContainer.input),
                )
            }
            .result

        div(
            child <--adder,
            stateContainer.bind,
            globalEvents --> globalState.input,
        )
