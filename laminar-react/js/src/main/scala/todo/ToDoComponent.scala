package todo

import com.raquo.laminar.api.L.{*, given}
import todo.model.{GlobalEvent, ToDo, ToDoList}


object ToDoComponent:
    final case class Props(toDo: ToDo, list: ToDoList, index: Int)

    enum Event:
        case Complete, Delete

    def apply(toDoSignal: Signal[Props]): HtmlElement =
        val completeEvents = EventBus[Event]()

        val globalEvents = completeEvents.events.withCurrentValueOf(toDoSignal).map:
            case (Event.Complete, Props(_, list, index)) => GlobalEvent.CompleteToDo(list, index)
            case (Event.Delete, Props(_, list, index)) => GlobalEvent.DeleteToDo(list, index)

        div(
            h4(text <-- toDoSignal.map(_._1.label)),
            child.maybe <-- toDoSignal.map(v => v._1.details.map(txt => div(txt))),
            div(
                display.flex,
                flexDirection.row,
                button("Complete", onClick.mapTo(Event.Complete) --> completeEvents.writer),
                button("Remove", onClick.mapTo(Event.Delete) --> completeEvents.writer),
            ),
            globalEvents --> globalState.input,
        )
