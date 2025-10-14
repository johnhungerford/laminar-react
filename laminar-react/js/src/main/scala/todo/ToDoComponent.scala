package todo

import com.raquo.laminar.api.L.{*, given}
import todo.model.{AppEvent, ToDo, ToDoList}
import common.StateContext
import common.styles.{Flex, Icon, card, customInput, makeIconButton}


/** Displays an in-progress to-do item, with collapsible details. Displays buttons for
  * completing and deleting item.
  */
object ToDoComponent:
    final case class Props(toDo: ToDo, list: ToDoList, index: Int)

    private enum State:
        case Collapsed, Expanded

        def reduce(event: StateEvent): State = event match {
            case StateEvent.Expand => State.Expanded
            case StateEvent.Collapse => State.Collapsed
            case _ => this
        }

    private enum StateEvent:
        case Complete, Delete, Expand, Collapse

    def apply(in: Signal[Props])(using appContext: AppContext): HtmlElement =
        // Initialize local state. Needs to be bound to the element (see below)
        val localContext = StateContext[State, StateEvent](
            State.Collapsed,
            (state, event) => state.reduce(event),
        )

        // Collect local events that should trigger global events, and transform them
        // accordingly. Needs to be bound to global store input (see below)
        val globalEvents = localContext.events.withCurrentValueOf(in).collect:
            case (StateEvent.Complete, Props(_, list, index)) => AppEvent.CompleteToDo(list, index)
            case (StateEvent.Delete, Props(_, list, index)) => AppEvent.DeleteToDo(list, index)

        card(
            Flex.column(
                Flex.row(
                    Flex.split,
                    Flex.row(
                        input(`type` := "checkbox", onClick.mapTo(StateEvent.Complete) --> localContext.input),
                        strong(text <-- in.map(_._1.label)),
                        // Don't really worry about rendering with splitOne or anything here because
                        // we're just rendering icon buttons
                        child <-- localContext.state.combineWith(in.map(_.toDo.details)).map:
                            case (_, None) => emptyNode
                            case (State.Collapsed, _) =>
                                Icon.chevronRight(makeIconButton, onClick.mapTo(StateEvent.Expand) --> localContext.input)
                            case (State.Expanded, _) =>
                                Icon.chevronDown(makeIconButton, onClick.mapTo(StateEvent.Collapse) --> localContext.input)
                    ),
                    Icon.close(makeIconButton, onClick.mapTo(StateEvent.Delete) --> localContext.input)
                ),
                // Don't worry about rendering here because it's just text (no internal state)
                child.maybe <-- in.combineWith(localContext.state).map:
                    case (Props(ToDo(_, Some(details)), _, _), State.Expanded) => Some(div(details))
                    case _ => None
            ),

            // Bind events
            globalEvents --> appContext.input,
            localContext.bind,
        )
