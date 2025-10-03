package todo

import todo.model.{GlobalEvent, GlobalState}
import util.StateContainer

type GlobalStore = StateContainer[GlobalState, GlobalEvent]
