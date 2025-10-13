package todo

import com.raquo.laminar.api.L.{*, given}
import common.style.{Flex, Icon, makeIconButton}
import todo.model.{AppEvent, ToDo, ToDoList, ToDoListState}


/** Displays a to-do list, including name, in-progress, and completed. Includes
  * a button deleting the list. Displays a component for creating to-dos at the top
  * of the in-progress list
  */
object ToDoListComponent:
    final case class Props(list: ToDoList, state: ToDoListState)

    def apply(in: Signal[Props])(using appContext: AppContext): HtmlElement =
        // Construct reactive ToDoComponent.Props
        val toDosSignal = in.map:
            case Props(list, ToDoListState(toDos, _)) =>
                toDos.zipWithIndex.map:
                    case (toDo, index) =>
                        ToDoComponent.Props(toDo, list, index)

        // Construct reactive CompletedToDoComponent.Props
        val completedToDosSignal = in.map:
            case Props(list, ToDoListState(_, doneToDos)) =>
                doneToDos.zipWithIndex.map:
                    case (toDo, index) =>
                        CompletedToDoComponent.Props(toDo, list, index)

        // Compose `onClick` with props signal to map to the appropriate AppEvent
        val onClickAsRemove = onClick.compose(_.withCurrentValueOf(in.map(_.list)).map {
            case (_, list) => AppEvent.DeleteList(list)
        })

        Flex.column(
            Flex.row(
                h2(text <-- in.map(v => s"List: ${v.list.name}")),
                Icon.close(makeIconButton, onClickAsRemove --> appContext.input)
            ),
            h3("In-progress"),
            child <-- CreateToDoComponent(in.map(props => CreateToDoComponent.Props(props.list, props.state.toDos.map(_.label).toSet))),
            children <-- toDosSignal.split(_.toDo.label) {
                (_, _, toDoSignal) => ToDoComponent(toDoSignal.distinct)
            },
            // Only show completed section if there are completed to-dos
            child <-- in.splitOne(_.state.completedToDos.nonEmpty) {
                case (false, _, _) => emptyNode
                case (true, _, _) => Flex.column(
                    h3("Completed"),
                    children <-- completedToDosSignal.split(_._1.label):
                        case (_, _, doneToDoSignal) => CompletedToDoComponent(doneToDoSignal.distinct)
                )
            },
        )
