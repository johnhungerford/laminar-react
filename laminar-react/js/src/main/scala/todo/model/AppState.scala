package todo.model

import scala.collection.immutable.ListMap

final case class AppState(
    selectedList: Option[ToDoList],
    lists: ListMap[ToDoList, ToDoListState]
):
    self =>
      def reduce(event: AppEvent): AppState = event match
          case AppEvent.NewList(name) =>
              if lists.exists(_._1.name == name) then self
              else
                  val newList = ToDoList(name)
                  copy(
                      selectedList = Some(newList),
                      lists = lists + (newList -> ToDoListState.initial),
                  )
          case AppEvent.DeleteList(list) =>
              copy(
                  selectedList = if selectedList.contains(list) then lists.keys.headOption else selectedList,
                  lists = lists - list,
              )
          case AppEvent.NewToDo(list, label, details) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                    Some(ToDoListState(toDos :+ ToDo(label, details), doneToDos))
              })
          case AppEvent.UpdateToDo(list, index, newLabel, newDetails) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      toDos.lift(index) match
                          case None => Some(ToDoListState(toDos, doneToDos))
                          case Some(ToDo(origLabel, origDetails)) =>
                              val updated = ToDo(newLabel.getOrElse(origLabel), newDetails.getOrElse(origDetails))
                              Some(ToDoListState(toDos.updated(index, updated), doneToDos))
              })
          case AppEvent.CompleteToDo(list, index) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      toDos.lift(index) match
                          case None => Some(ToDoListState(toDos, doneToDos))
                          case Some(toDo) =>
                              Some(ToDoListState(toDos.patch(index, Nil, 1), toDo +: doneToDos))
              })
          case AppEvent.RestoreToDo(list, index) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      doneToDos.lift(index) match
                          case None => Some(ToDoListState(toDos, doneToDos))
                          case Some(toDo) =>
                              Some(ToDoListState(toDos :+ toDo, doneToDos.patch(index, Nil, 1)))
              })
          case AppEvent.DeleteToDo(list, index) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      toDos.lift(index) match
                          case None => Some(ToDoListState(toDos, doneToDos))
                          case Some(toDo) =>
                              Some(ToDoListState(toDos.patch(index, Nil, 1), doneToDos))
              })
          case AppEvent.DeleteCompletedToDo(list, index) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      doneToDos.lift(index) match
                          case None => Some(ToDoListState(toDos, doneToDos))
                          case Some(toDo) =>
                              Some(ToDoListState(toDos, doneToDos.patch(index, Nil, 1)))
              })
          case AppEvent.CompleteAll(list) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      Some(ToDoListState(Vector.empty, toDos ++ doneToDos))
              })
          case AppEvent.ClearAllCompleted(list) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      Some(ToDoListState(toDos, Vector.empty))
              })
          case AppEvent.RestoreAll(list) =>
              copy(lists = lists.updatedWith(list) {
                  case None => None
                  case Some(ToDoListState(toDos, doneToDos)) =>
                      Some(ToDoListState(doneToDos ++ toDos, Vector.empty))
              })
          case todo.model.AppEvent.SelectList(list) =>
              copy(selectedList = Some(list))

object AppState:
    val initial: AppState = AppState(None, ListMap.empty)

final case class ToDoList(name: String)

final case class ToDoListState(
                                  toDos: Vector[ToDo],
                                  completedToDos: Vector[ToDo],
)

object ToDoListState:
    val initial: ToDoListState = ToDoListState(Vector.empty, Vector.empty)

final case class ToDo(label: String, details: Option[String])
