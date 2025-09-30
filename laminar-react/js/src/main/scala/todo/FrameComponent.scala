package todo

import com.raquo.laminar.api.L.{*, given}
import todo.model.{GlobalState, ToDo, ToDoList, ToDoListState}
import todo.util.routeSignal


object FrameComponent:
    def apply(): HtmlElement =
        val chooseListProps = globalState.state.map:
            case GlobalState(selectedList, lists) =>
                ChooseListComponent.Props(lists.keys.toSeq)

        val toDoListComponent: Signal[HtmlElement] = globalState
            .state
            .map(v => (v.selectedList, v.selectedList.flatMap(sl => v.lists.get(sl))))
            .routeSignal({
                case (Some(selectedList), Some(listState)) =>
                    ToDoListComponent.Props(selectedList, listState)
            })(
                toDoListPropsSignal => ToDoListComponent(toDoListPropsSignal)
            )
            .result(div())

        div(
            ChooseListComponent(chooseListProps),
            child <-- toDoListComponent,
            globalState.bind,
        )
