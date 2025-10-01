package todo.model

import scala.collection.immutable.ListMap

final case class GlobalState(
    selectedList: Option[ToDoList],
    lists: ListMap[ToDoList, ToDoListState]
):
    self =>
      def reduce(event: GlobalEvent): GlobalState = event match
          case GlobalEvent.NewList(name) =>
              if lists.exists(_._1.name == name) then self
              else
                  val newList = ToDoList(name)
                  copy(
                      selectedList = Some(newList),
                      lists = lists + (newList -> ToDoListState.initial),
                  )
          case GlobalEvent.DeleteList(list) =>
              copy(lists = lists - list)
          case GlobalEvent.NewToDo(list, label, details) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                    Some(ToDoListState(toDos :+ ToDo(label, details), doneToDos))
              })
          case GlobalEvent.UpdateToDo(list, index, newLabel, newDetails) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      toDos.lift(index) match
                          case None => Some(ToDoListState(toDos, doneToDos))
                          case Some(ToDo(origLabel, origDetails)) =>
                              val updated = ToDo(newLabel.getOrElse(origLabel), newDetails.getOrElse(origDetails))
                              Some(ToDoListState(toDos.updated(index, updated), doneToDos))
              })
          case GlobalEvent.CompleteToDo(list, index) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      toDos.lift(index) match
                          case None => Some(ToDoListState(toDos, doneToDos))
                          case Some(toDo) =>
                              Some(ToDoListState(toDos.patch(index, Nil, 1), toDo +: doneToDos))
              })
          case GlobalEvent.RestoreToDo(list, index) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      doneToDos.lift(index) match
                          case None => Some(ToDoListState(toDos, doneToDos))
                          case Some(toDo) =>
                              Some(ToDoListState(toDos :+ toDo, doneToDos.patch(index, Nil, 1)))
              })
          case GlobalEvent.DeleteToDo(list, index) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      toDos.lift(index) match
                          case None => Some(ToDoListState(toDos, doneToDos))
                          case Some(toDo) =>
                              Some(ToDoListState(toDos.patch(index, Nil, 1), doneToDos))
              })
          case GlobalEvent.DeleteCompletedToDo(list, index) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      doneToDos.lift(index) match
                          case None => Some(ToDoListState(toDos, doneToDos))
                          case Some(toDo) =>
                              Some(ToDoListState(toDos, doneToDos.patch(index, Nil, 1)))
              })
          case GlobalEvent.CompleteAll(list) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      Some(ToDoListState(Vector.empty, toDos ++ doneToDos))
              })
          case GlobalEvent.ClearAllCompleted(list) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      Some(ToDoListState(toDos, Vector.empty))
              })
          case GlobalEvent.RestoreAll(list) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      Some(ToDoListState(doneToDos ++ toDos, Vector.empty))
              })
          case todo.model.GlobalEvent.SelectList(list) =>
              copy(selectedList = Some(list))

object GlobalState:
    val initial: GlobalState = GlobalState(None, ListMap.empty)

final case class ToDoList(name: String)

final case class ToDoListState(
    toDos: Vector[ToDo],
    doneToDos: Vector[ToDo],
)

object ToDoListState:
    val initial: ToDoListState = ToDoListState(Vector.empty, Vector.empty)

final case class ToDo(label: String, details: Option[String])
