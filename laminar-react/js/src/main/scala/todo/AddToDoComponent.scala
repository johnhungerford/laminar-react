package todo

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import org.scalajs.dom.HTMLInputElement
import todo.model.{GlobalEvent, ToDo, ToDoList}
import util.routeSignal
import util.StateContainer
import io.github.nguyenyou.webawesome.laminar.{Button, Input, Checkbox, Divider, Icon, Select, UOption}


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

    def apply(propsSignal: Signal[Props])(using globalState: GlobalStore): Signal[HtmlElement] =
        propsSignal.map(_.list).distinct.flatMapSwitch: currentList =>
            val localState = StateContainer[State, Event](
                State.Initial,
                (state, event) => state.reduce(event)
            )

            val globalEvents = localState.events.withCurrentValueOf(propsSignal, localState.state).collect:
                case (Event.Add, Props(list, _), State.Adding(label, details)) =>
                    println(s"Adding $label with details: $details")
                    val detailsOpt = if details.strip().isEmpty then None else Some(details.strip())
                    GlobalEvent.NewToDo(list, label.strip(), detailsOpt)

            localState.state.combineWith(propsSignal)
                .routeSignal({ case (State.Initial, _) => () }) { _ =>
                    div(
                        Button(
                            _.slots.start(Icon(_.name := "add", _.label := "close")()),
                            _.appearance.plain,
                        )(
                            "Add todo",
                            onClick.mapTo(Event.StartAdding) --> localState.input,
                            localState.bind,
                            globalEvents --> globalState.input,
                        )
                    )
                }
                .routeSignal({ case (st: State.Adding, pr: Props) => (st, pr) }) { signal =>
                    val addingSignal = signal.map(_._1: State.Adding)

                    val addDisabled = signal.map:
                        case (State.Adding(label, _), Props(_, existingLabels)) =>
                            dom.console.log(existingLabels)
                            label.strip().isEmpty || existingLabels.contains(label.strip())

                    div(
                        className := "wa-stack",
                        Input(
                            _.label := "Label",
                        )(
                            value <-- addingSignal.map(_.labelText),
                            onInput.map(v => Event.SetLabel(v.currentTarget.asInstanceOf[HTMLInputElement].value)) --> localState.input,
                        ),
                        Input(
                            _.label := "Details",
                        )(
                            value <-- addingSignal.map(_.detailsText),
                            onInput.map(v => Event.SetDetails(v.currentTarget.asInstanceOf[HTMLInputElement].value)) --> localState.input,
                        ),
                        div(
                            className := "wa-cluster",
                            Button(_.appearance.filled)("Add", disabled <-- addDisabled, onClick.mapTo(Event.Add) --> localState.input),
                            Button(_.appearance.filled)("Cancel", onClick.mapTo(Event.StopAdding) --> localState.input),
                        ),

                        // Bind events
                        onMountFocus,
                        localState.bind,
                        globalEvents --> globalState.input,
                    )
                }
                .result
