package util

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{*, given}

/**
 * State management. Keeps state, manages updates via input events, and allows
 * subscriptions to events and state updates
 */
trait StateContainer[State, Event]:
    def input: Sink[Event]
    def events: EventStream[Event]
    def state: Signal[State]
    def bind: Binder[Element]

object StateContainer:
    /**
     * Implement a state container
     * @param initialState initial state
     * @param reduce function defining how state is updated by an event
     */
    def apply[State, Event](
        initialState: State,
        reduce: (State, Event) => State
    ): StateContainer[State, Event] = new:
        private val _state = Var(initialState)
        private val _stateUpdates = _state.updater(reduce)
        private val _events = EventBus[Event]()

        override val input: Sink[Event] =
            _events.writer

        override val events: EventStream[Event] =
            _events.events

        override val state: Signal[State] =
            _state.signal

        override val bind: Binder[Element] =
            _events.events --> _stateUpdates
