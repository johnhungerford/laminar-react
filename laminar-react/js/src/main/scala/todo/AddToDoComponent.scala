package todo

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import org.scalajs.dom.HTMLInputElement
import todo.model.{GlobalEvent, ToDo, ToDoList}
import util.routeSignal
import util.StateContainer


object AddToDoComponent:
    final case class Props(list: ToDoList, existingLabels: Set[String])

    enum State:
        case Initial
        case Adding(labelText: String, detailsText: String)

        def reduce(event: Event): State = event match
            case Event.StartAdding => Adding("", "")
            case Event.StopAdding => Initial
            case Event.SetLabel(value) => this match
                case State.Adding(_, detailsText) => State.Adding(value, detailsText)
                case _ => this
            case Event.SetDetails(value) => this match
                case State.Adding(labelText, _) => State.Adding(labelText, value)
                case _ => this
            case Event.Add => Initial

    enum Event:
        case StartAdding, StopAdding, Add
        case SetLabel(value: String)
        case SetDetails(value: String)

    def apply(propsSignal: Signal[Props]): Signal[HtmlElement] =
        propsSignal.map(_.list).distinct.flatMapSwitch: currentList =>
            val stateContainer = StateContainer[State, Event](
                State.Initial,
                (state, event) => state.reduce(event)
            )

            val globalEvents = stateContainer.events.withCurrentValueOf(propsSignal, stateContainer.state).collect:
                case (Event.Add, Props(list, _), State.Adding(label, details)) =>
                    println(s"Adding $label with details: $details")
                    val detailsOpt = if details.strip().isEmpty then None else Some(details.strip())
                    GlobalEvent.NewToDo(list, label.strip(), detailsOpt)

            stateContainer.state.combineWith(propsSignal)
                .routeSignal({ case (State.Initial, _) => () }) { _ =>
                    button(
                        "Add todo",
                        onClick.mapTo(Event.StartAdding) --> stateContainer.input,
                        stateContainer.bind,
                        globalEvents --> globalState.input,
                    )
                }
                .routeSignal({ case (st: State.Adding, pr: Props) => (st, pr) }) { signal =>
                    val addingSignal = signal.map(_._1: State.Adding)

                    val addDisabled = signal.map:
                        case (State.Adding(label, _), Props(_, existingLabels)) =>
                            dom.console.log(existingLabels)
                            label.strip().isEmpty || existingLabels.contains(label.strip())

                    div(
                        div(
                            "Label",
                            input(
                                controlled(
                                    value <-- addingSignal.map(_.labelText),
                                    onInput.map(v => Event.SetLabel(v.currentTarget.asInstanceOf[HTMLInputElement].value)) --> stateContainer.input,
                                )
                            ),
                        ),
                        div(
                            "Details",
                            input(
                                controlled(
                                    value <-- addingSignal.map(_.detailsText),
                                    onInput.map(v => Event.SetDetails(v.currentTarget.asInstanceOf[HTMLInputElement].value)) --> stateContainer.input,
                                )
                            ),
                        ),
                        button("Add", disabled <-- addDisabled, onClick.mapTo(Event.Add) --> stateContainer.input),
                        button("Exit", onClick.mapTo(Event.StopAdding) --> stateContainer.input),

                        stateContainer.bind,
                        globalEvents --> globalState.input,
                    )
                }
                .result
