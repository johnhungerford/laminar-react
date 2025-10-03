package todo

import com.raquo.laminar.api.L.{*, given}
import todo.model.{GlobalEvent, ToDo, ToDoList}
import io.github.nguyenyou.webawesome.laminar.{Button, Card, Checkbox, Divider, Icon, Select, UOption}


object DoneToDoComponent:
    final case class Props(toDo: ToDo, list: ToDoList, index: Int)

    enum Event:
        case Restore, Delete

    def apply(propsSignal: Signal[Props])(using globalState: GlobalStore): HtmlElement =
        val completeEvents = EventBus[Event]()

        val onClickMappedToIndex = onClick.compose(
            _.withCurrentValueOf(propsSignal).map:
                case (_, Props(_, _, index)) => index
        )

        val globalEvents = completeEvents.events.withCurrentValueOf(propsSignal).map:
            case (Event.Restore, Props(_, list, index)) => GlobalEvent.RestoreToDo(list, index)
            case (Event.Delete, Props(_, list, index)) => GlobalEvent.DeleteCompletedToDo(list, index)

        div(
            className := "wa-split",
            div(
                className := "wa-cluster",
                Button(_.appearance.plain)(Icon(_.name := "arrows-rotate", _.label := "restore")(), onClick.mapTo(Event.Restore) --> completeEvents.writer),
                div(text <-- propsSignal.map(_._1.label)),
            ),
            Button(_.appearance.plain)(Icon(_.name := "xmark", _.label := "close")(), onClick.mapTo(Event.Delete) --> completeEvents.writer),
            globalEvents --> globalState.input,
        )
