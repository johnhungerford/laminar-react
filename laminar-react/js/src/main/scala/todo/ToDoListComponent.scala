package todo

import com.raquo.laminar.api.L.{*, given}
import todo.model.{ToDo, ToDoList, ToDoListState}


object ToDoListComponent:
    final case class Props(list: ToDoList, state: ToDoListState)

    def apply(listSignal: Signal[Props]): HtmlElement =
        val toDosSignal = listSignal.map:
            case Props(list, ToDoListState(toDos, doneToDos)) =>
                toDos.zipWithIndex.map:
                    case (toDo, index) =>
                        ToDoComponent.Props(toDo, list, index)

        div(
            h2(text <-- listSignal.map(_.list.name)),
            h3("To Do"),
            child <-- AddToDoComponent(listSignal.map(props => AddToDoComponent.Props(props.list, props.state.toDos.map(_.label).toSet))),
            children <-- toDosSignal.split(_.toDo.label) {
                (_, _, toDoSignal) => ToDoComponent(toDoSignal.distinct)
            },
            h3("Done"),
            children <-- listSignal.map(_.state.doneToDos).split(_.label):
                case (label, _, _) => span(label, marginRight := "10px")
        )
