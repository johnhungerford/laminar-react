package todo

import com.raquo.laminar.api.L.{*, given}
import todo.model.{GlobalEvent, ToDo, ToDoList, ToDoListState}
import io.github.nguyenyou.webawesome.laminar.{Button, Card, Checkbox, Divider, Icon, Select, UOption}


object ToDoListComponent:
    final case class Props(list: ToDoList, state: ToDoListState)

    def apply(propsSignal: Signal[Props]): HtmlElement =
        val toDosSignal = propsSignal.map:
            case Props(list, ToDoListState(toDos, _)) =>
                toDos.zipWithIndex.map:
                    case (toDo, index) =>
                        ToDoComponent.Props(toDo, list, index)

        val doneToDosSignal = propsSignal.map:
            case Props(list, ToDoListState(_, doneToDos)) =>
                doneToDos.zipWithIndex.map:
                    case (toDo, index) =>
                        DoneToDoComponent.Props(toDo, list, index)

        val mappedRemoveListEvent = onClick.compose(_.withCurrentValueOf(propsSignal.map(_.list)).map {
            case (_, list) => GlobalEvent.DeleteList(list)
        })

        div(
            className := "wa-stack",
            div(
                className := "wa-cluster",
                h3(text <-- propsSignal.map(_.list.name)),
                Button(_.appearance.plain)(Icon(_.name := "xmark", _.label := "close")(mappedRemoveListEvent --> globalState.input)),
            ),
            h4("To Do"),
            child <-- AddToDoComponent(propsSignal.map(props => AddToDoComponent.Props(props.list, props.state.toDos.map(_.label).toSet))),
            children <-- toDosSignal.split(_.toDo.label) {
                (_, _, toDoSignal) => ToDoComponent(toDoSignal.distinct)
            },
            h3("Done"),
            children <-- doneToDosSignal.split(_._1.label):
                case (_, _, doneToDoSignal) => DoneToDoComponent(doneToDoSignal.distinct)
        )
