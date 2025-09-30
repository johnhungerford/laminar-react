package todo.util

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{*, given}

trait StateContainer[State, Event]:
    def input: Sink[Event]
    def events: EventStream[Event]
    def state: Signal[State]
    def bind: Binder[Element]

object StateContainer:
    def apply[State, Event](
        initialState: State,
        reduce: (State, Event) => State
    ): StateContainer[State, Event] = new:
        private val _state = Var(initialState)
        private val _stateUpdates = _state.updater(reduce)
        private val _events = EventBus[Event]()

        override def input: Sink[Event] =
            _events.writer

        override def events: EventStream[Event] =
            _events.events

        override def state: Signal[State] =
            _state.signal

        override def bind: Binder[Element] =
            _events.events --> _stateUpdates
