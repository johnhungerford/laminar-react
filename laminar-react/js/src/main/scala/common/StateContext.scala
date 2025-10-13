package common

import com.raquo.laminar.api.L.{*, given}

/**
 * State management wrapper. Keeps state, manages updates via input events, and supports
 * subscriptions to events and state updates
 *
 * @param initialState
 *   Initial state of your store: required to broadcast state as a [[Signal]]
 * @param reduce
 *   Reducer function that updates state on each event
 */
class StateContext[State, Event](
    initialState: State,
    reduce: (State, Event) => State
):
    private val stateVar = Var(initialState)
    private val stateUpdater = stateVar.updater(reduce)
    private val eventBus = EventBus[Event]()

    /** Handler for events.
      * Usage:
      *   button("Click me", onClick.mapTo(SomeEvent) --> stateCtx.input)
      */
    def input: Sink[Event] = eventBus.writer

    /** Provides access to all events. Can be used for logging events or to send
      * events to a service for further processing (e.g., event sourcing)
      */
    def events: EventStream[Event] = eventBus.events

    /** Signal publishing latest version of state.
      * Usage:
      *   h1(text <-- stateCtx.state.map(_.path.to.headerText))
      */
    def state: Signal[State] = stateVar.signal

    /** Initialize state, propagating events through the reducer. Add this
      * to an element with desired lifetime of your state.
      */
    def bind: Binder[Element] = eventBus.events --> stateUpdater
