package todo

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import common.styles.Flex
import todo.model.{AppEvent, AppState, ToDo, ToDoList, ToDoListState}
import common.StateContext
import org.scalajs.dom


/** Top-level component for To-Do App. Initializes global state and passes
  * implicitly to the rest of the component tree.
  */
object AppComponent:
    def apply(): HtmlElement =
        // Initialize the global state of the application and provide it
        // as an implicit parameter to any components that need it.
        // Needs to be bound to the element (see below)
        given appContext: AppContext = StateContext[AppState, AppEvent](
            AppState.initial,
            (state, event) => state.reduce(event),
        )

        val chooseListProps = appContext.state.map:
            case AppState(selectedList, lists) =>
                ChooseListComponent.Props(selectedList, lists.keys.toSeq)

        val toDoListComponent: Signal[Node] = appContext
            .state
            .map(v => (v.selectedList, v.selectedList.flatMap(sl => v.lists.get(sl))))
            .splitMatchOne
            .handleCase({
                case (Some(selectedList: ToDoList), Some(listState: ToDoListState)) =>
                    ToDoListComponent.Props(selectedList, listState)
            })(
                (_, toDoListPropsSignal) => ToDoListComponent(toDoListPropsSignal)
            )
            .handleType[Any]((_, _) => emptyNode) // Default case
            .toSignal

        Flex.column(
            padding := "30px",
            ChooseListComponent(chooseListProps),
            child <-- toDoListComponent,

            // Bind app events to app state
            appContext.bind,

            // Log all events
            appContext.events --> { evt =>
                dom.console.log(evt.toString)
            }
        )
