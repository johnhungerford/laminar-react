package todo

import todo.model.{AppEvent, AppState}
import common.StateContext

type AppContext = StateContext[AppState, AppEvent]
