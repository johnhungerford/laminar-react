package todo

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.HTMLSelectElement
import todo.model.{GlobalEvent, ToDo, ToDoList, ToDoListState}


object ChooseListComponent:
    final case class Props(lists: Seq[ToDoList])

    def apply(listSignal: Signal[Props]): HtmlElement =
        val options = listSignal.map(_.lists).split(_.name):
            case (name, list, _) => option(name, value := name)

        div(
            select(
                onChange.map(v => GlobalEvent.SelectList(ToDoList(v.currentTarget.asInstanceOf[HTMLSelectElement].value))) --> globalState.input,
                children <-- options,
            ),
            AddListComponent(listSignal.map(v => AddListComponent.Props(v.lists.map(_.name).toSet))),
        )
