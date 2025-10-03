package todo

import com.raquo.laminar.api.L.{*, given}
import todo.model.{GlobalEvent, ToDo, ToDoList}
import io.github.nguyenyou.webawesome.laminar.{Button, Card, Checkbox, Divider, Icon, Select, UOption}
import util.StateContainer


object ToDoComponent:
    final case class Props(toDo: ToDo, list: ToDoList, index: Int)

    enum State:
        case Collapsed, Expanded

        def reduce(event: Event): State = event match {
            case Event.Expand => State.Expanded
            case Event.Collapse => State.Collapsed
            case _ => this
        }

    enum Event:
        case Complete, Delete, Expand, Collapse

    def apply(propsSignal: Signal[Props])(using globalState: GlobalStore): HtmlElement =
        val localState = StateContainer[State, Event](
            State.Collapsed,
            (state, event) => {
                println(s"$state <-- $event")
                state.reduce(event)
            },
        )

        val globalEvents = localState.events.withCurrentValueOf(propsSignal).collect:
            case (Event.Complete, Props(_, list, index)) => GlobalEvent.CompleteToDo(list, index)
            case (Event.Delete, Props(_, list, index)) => GlobalEvent.DeleteToDo(list, index)

        Card()(
            div(
                className := "wa-stack",
                div(
                    className := "wa-split",
                    div(
                        className := "wa-cluster",
                        Checkbox()(onClick.mapTo(Event.Complete) --> localState.input),
                        strong(text <-- propsSignal.map(_._1.label)),
                        child <-- localState.state.combineWith(propsSignal.map(_.toDo.details)).map:
                            case (_, None) => emptyNode
                            case (State.Collapsed, _) =>
                                Button(_.appearance.plain)(Icon(_.name := "chevron-right", _.label := "expand")(), onClick.mapTo(Event.Expand) --> localState.input)
                            case (State.Expanded, _) =>
                                Button(_.appearance.plain)(Icon(_.name := "chevron-down", _.label := "collapse")(), onClick.mapTo(Event.Collapse) --> localState.input),
                    ),
                    Button(_.appearance.plain)(Icon(_.name := "xmark", _.label := "close")(), onClick.mapTo(Event.Delete) --> localState.input),
                ),
                child.maybe <-- propsSignal.combineWith(localState.state).map:
                    case (Props(ToDo(_, Some(details)), _, _), State.Expanded) => Some(div(details))
                    case _ => None
            ),
            localState.bind,
            globalEvents --> globalState.input,
        )
