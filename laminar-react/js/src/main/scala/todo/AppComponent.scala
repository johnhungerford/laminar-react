package todo

import com.raquo.laminar.api.L.{*, given}
import todo.model.{GlobalState, ToDo, ToDoList, ToDoListState}
import util.routeSignal
import io.github.nguyenyou.webawesome.laminar.{Divider, Select, UOption, Card}

/** Top-level component for To-Do App.
  */
object AppComponent:
    def apply(): HtmlElement =
        val chooseListProps = globalState.state.map:
            case GlobalState(selectedList, lists) =>
                ChooseListComponent.Props(selectedList, lists.keys.toSeq)

        val toDoListComponent: Signal[Node] = globalState
            .state
            .map(v => (v.selectedList, v.selectedList.flatMap(sl => v.lists.get(sl))))
            .routeSignal({
                case (Some(selectedList), Some(listState)) =>
                    ToDoListComponent.Props(selectedList, listState)
            })(
                toDoListPropsSignal => ToDoListComponent(toDoListPropsSignal)
            )
            .result(emptyNode)

        div(
            className := "wa-stack",
            ChooseListComponent(chooseListProps),
            child <-- toDoListComponent,
            globalState.bind,
        )
