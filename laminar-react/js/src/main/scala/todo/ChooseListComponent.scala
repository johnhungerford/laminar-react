package todo

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.HTMLSelectElement
import todo.model.{GlobalEvent, ToDo, ToDoList, ToDoListState}
import util.routeSignal
import io.github.nguyenyou.webawesome.laminar.{Divider, Select, UOption, Card}


object ChooseListComponent:
    final case class Props(selectedList: Option[ToDoList], lists: Seq[ToDoList])

    def apply(propsSignal: Signal[Props])(using globalState: GlobalStore): HtmlElement =
        div(
            className := "wa-stack",
            child <-- propsSignal
                .routeSignal({ case Props(_, lists ) if lists.nonEmpty => lists }) { listsSignal =>
                    val options = listsSignal.split(_.name):
                        case (name, list, _) => UOption()(name, value := name)

                    Select(
                        _.label := "Select list"
                    )(
                        child <-- propsSignal.map(_.selectedList.isEmpty).distinct.map(
                            if _ then UOption()("Select a list", value := "none")
                            else emptyNode
                        ),
                        children <-- options,
                        value <-- propsSignal.map:
                            case Props(None, _) => "none"
                            case Props(Some(ToDoList(name)), _) => name,
                        onChange.mapToValue.filter(_ != "none").map(v => GlobalEvent.SelectList(ToDoList(v))) --> globalState.input,
                    )
                }
                .result(emptyNode)
            ,
            AddListComponent(propsSignal.map(v => AddListComponent.Props(v.lists.map(_.name).toSet))),
        )
