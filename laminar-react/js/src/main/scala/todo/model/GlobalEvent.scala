package todo.model

enum GlobalEvent:
    case SelectList(list: ToDoList)
    case NewList(name: String)
    case DeleteList(list: ToDoList)
    case NewToDo(list: ToDoList, label: String, details: Option[String])
    case UpdateToDo(list: ToDoList, index: Int, newLabel: Option[String], newDetails: Option[Option[String]])
    case CompleteToDo(list: ToDoList, index: Int)
    case RestoreToDo(list: ToDoList, index: Int)
    case DeleteToDo(list: ToDoList, index: Int)
    case DeleteCompletedToDo(list: ToDoList, index: Int)
    case CompleteAll(list: ToDoList)
    case ClearAllCompleted(list: ToDoList)
    case RestoreAll(list: ToDoList)
