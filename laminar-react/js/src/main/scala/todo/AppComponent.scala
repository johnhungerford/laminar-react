package todo

import com.raquo.laminar.api.L.{*, given}
import todo.model.{GlobalEvent, GlobalState, ToDo, ToDoList, ToDoListState}
import util.{StateContainer, routeSignal}
import io.github.nguyenyou.webawesome.laminar.{Card, Divider, Select, UOption}

/** Top-level component for To-Do App.
  */
object AppComponent:
    def apply(): HtmlElement =
        // Initialize the global state of the application and provide it
        // as an implicit parameter to any components that need it
        given globalState: GlobalStore = StateContainer[GlobalState, GlobalEvent](
            GlobalState.initial,
            (state, event) => state.reduce(event),
        )

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
