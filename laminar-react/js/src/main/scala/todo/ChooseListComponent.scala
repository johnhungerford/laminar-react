package todo

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.HTMLSelectElement
import todo.model.{AppEvent, ToDo, ToDoList, ToDoListState}
import common.styles.{Flex, customOption, customSelect}


/** Component for selecting a list from all existing lists (only shows selection if
  * lists exist). Displays a component for creating a new list at the bottom.
  */
object ChooseListComponent:
    final case class Props(selectedList: Option[ToDoList], lists: Seq[ToDoList])

    def apply(in: Signal[Props])(using appContext: AppContext): HtmlElement = {
        // This event bus is needed for an interesting reason: the select
        // element below receives its "value" from the `propsSignal` independently
        // from the `children` receiver getting new options element from `propSignal`.
        // The result is that the select value can be set to an invalid value as the
        // corresponding change has not been reflected in the options dom array. We
        // therefore need to signal when those options are changed and reset the value
        // in case it failed. See `onMountUnmountCallback` below on the `customOption`
        // element below and the `value <--` modifier on `customSelect` further down.
        val checkSelectValueEvents = EventBus[Unit]()
        val checkSelectValueSignal = checkSelectValueEvents.events.toSignal(())

        Flex.column(
            child <-- { in
                .splitMatchOne
                .handleCase({ case Props(_, lists ) if lists.nonEmpty => lists }) { (_, listsSignal) =>
                    val options = listsSignal.split(_.name):
                        case (name, list, _) =>
                            customOption(
                                name,
                                value := name,
                                // Signal that an option has been added or removed
                                onMountUnmountCallback(
                                    _ => checkSelectValueEvents.writer.onNext(()),
                                    _ => checkSelectValueEvents.writer.onNext(()),
                                ),
                            )

                    Flex.row(
                        "Select list",
                        customSelect(
                            child <-- in.map(_.selectedList.isEmpty).distinct.map(
                                if _ then customOption("Select a list", value := "none")
                                else emptyNode
                            ),
                            children <-- options,
                            controlled(
                                value <-- in.combineWith(checkSelectValueSignal).map:
                                    case Props(None, _) => "none"
                                    case Props(Some(ToDoList(name)), _) => name,
                                onChange.mapToValue.filter(_ != "none").map(v => AppEvent.SelectList(ToDoList(v))) --> appContext.input,
                            ),
                        )
                    )
                }
                .handleType[Any]((_, _) => emptyNode) // Default case
                .toSignal
            },
            CreateListComponent(in.map(v => CreateListComponent.Props(v.lists.map(_.name).toSet))),
        )
    }
