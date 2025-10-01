package todo

import com.raquo.laminar.api.L.{*, given}
import todo.model.{GlobalEvent, ToDo, ToDoList}


object DoneToDoComponent:
    final case class Props(toDo: ToDo, list: ToDoList, index: Int)

    enum Event:
        case Restore, Delete

    def apply(propsSignal: Signal[Props]): HtmlElement =
        val completeEvents = EventBus[Event]()

        val globalEvents = completeEvents.events.withCurrentValueOf(propsSignal).map:
            case (Event.Restore, Props(_, list, index)) => GlobalEvent.RestoreToDo(list, index)
            case (Event.Delete, Props(_, list, index)) => GlobalEvent.DeleteCompletedToDo(list, index)

        div(
            div(
                display.flex,
                flexDirection.row,
                alignItems.center,
                p(text <-- propsSignal.map(_._1.label)),
                button("Restore", onClick.mapTo(Event.Restore) --> completeEvents.writer),
                button("Remove", onClick.mapTo(Event.Delete) --> completeEvents.writer),
            ),
            globalEvents --> globalState.input,
        )
