package todo

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.HTMLSelectElement
import todo.model.{GlobalEvent, ToDo, ToDoList, ToDoListState}


object ChooseListComponent:
    final case class Props(lists: Seq[ToDoList])

    def apply(propsSignal: Signal[Props]): HtmlElement =
        val options = propsSignal.map(_.lists).split(_.name):
            case (name, list, _) => option(name, value := name)

        div(
            select(
                onChange.map(v => GlobalEvent.SelectList(ToDoList(v.currentTarget.asInstanceOf[HTMLSelectElement].value))) --> globalState.input,
                children <-- options,
            ),
            AddListComponent(propsSignal.map(v => AddListComponent.Props(v.lists.map(_.name).toSet))),
        )
