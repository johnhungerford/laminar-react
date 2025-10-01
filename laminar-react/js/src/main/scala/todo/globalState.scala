package todo

import com.raquo.laminar.api.L.{*, given}
import todo.model.{GlobalEvent, GlobalState}
import util.StateContainer

/** Global state store. See [[StateContainer]]
  */
val globalState: StateContainer[GlobalState, GlobalEvent] = StateContainer[GlobalState, GlobalEvent](
    GlobalState.initial,
    (state, event) => state.reduce(event),
)
