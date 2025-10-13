package todo

import com.raquo.laminar.api.L.{*, given}
import common.style.{Flex, Icon, customButton, makeIconButton}
import todo.model.{AppEvent, ToDo, ToDoList}


/** Render a completed component. Do not show details; provide buttons for
  * restoring and deleting.
  */
object CompletedToDoComponent:
    final case class Props(toDo: ToDo, list: ToDoList, index: Int)

    private enum Event:
        case Restore, Delete

    def apply(in: Signal[Props])(using appContext: AppContext): HtmlElement =
        val completeEvents = EventBus[Event]()

        // Collect local events that should trigger global events, and transform them
        // accordingly. Needs to be bound to global store input (see below)
        val globalEvents = completeEvents.events.withCurrentValueOf(in).map:
            case (Event.Restore, Props(_, list, index)) => AppEvent.RestoreToDo(list, index)
            case (Event.Delete, Props(_, list, index)) => AppEvent.DeleteCompletedToDo(list, index)

        Flex.row(
            Flex.split,
            Flex.row(
                Icon.refresh(makeIconButton, onClick.mapTo(Event.Restore) --> completeEvents.writer),
                div(text <-- in.map(_._1.label)),
            ),
            Icon.close(makeIconButton, onClick.mapTo(Event.Delete) --> completeEvents.writer),

            // Bind events
            globalEvents --> appContext.input,
        )
