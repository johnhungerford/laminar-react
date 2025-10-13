package todo

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.HTMLInputElement
import todo.model.AppEvent
import common.StateContext
import common.style.{Flex, card, customButton, customInput}


/** A component to create a new list. Displays a prompt button; when click
  * displays a text field for the new list's name
  */
object CreateListComponent:
    final case class Props(existingNames: Set[String])

    private enum StateEvent:
        case StartCreating
        case StopCreating
        case SetNameText(value: String)
        case Create

    private enum State:
        case Initial
        case Creating(nameText: String)

        def reduce(event: StateEvent): State = event match
            case StateEvent.StartCreating => Creating("")
            case StateEvent.StopCreating => Initial
            case StateEvent.SetNameText(value) => Creating(value)
            case StateEvent.Create => Initial

    def apply(in: Signal[Props])(using appContext: AppContext): HtmlElement =
        // Initialize local state. Needs to be bound to the element (see below)
        val localContext = StateContext[State, StateEvent](
            State.Initial,
            (state, event) => state.reduce(event),
        )

        // Collect local events that should trigger global events, and transform them
        // accordingly. Needs to be bound to global store input (see below)
        val globalEvents = localContext.events.withCurrentValueOf(localContext.state, in).collect:
            case (StateEvent.Create, State.Creating(name), Props(existingNames)) if !existingNames.contains(name.strip()) =>
                AppEvent.NewList(name.strip())
            case (StateEvent.Create, State.Creating(name), _) =>
                AppEvent.NewList(name.strip())

        val adder = localContext.state.combineWith(in)
            .splitMatchOne
            // Initial state: render a button to add a new list
            .handleCase({ case (State.Initial, _) => () }) { (_, _) =>
                customButton(
                    "New list",
                    onClick.mapTo(StateEvent.StartCreating) --> localContext.input,
                )
            }
            // Once button above has been pressed, render a form for
            // creating a new list
            // Note you need type annotations on your match expression due
            // underlying macro implementation
            .handleCase({ case (creatingState: State.Creating, props: Props) => (creatingState, props) }) { (_, signal) =>
                val addingSignal = signal.map(_._1)

                val addDisabled: Signal[Boolean] = signal.map:
                    case (State.Creating(nameText), Props(existingNames)) =>
                        val stripped = nameText.strip()
                        stripped.isEmpty || existingNames.contains(stripped)

                card(
                    Flex.column(
                        h4("New list"),
                        customInput(
                            value <-- addingSignal.map(_.nameText),
                            onInput.mapToValue.map(StateEvent.SetNameText(_)) --> localContext.input,
                            onMountFocus,
                        ),
                        Flex.row(
                            customButton("Add", disabled <-- addDisabled, onClick.mapTo(StateEvent.Create) --> localContext.input),
                            customButton("Cancel", onClick.mapTo(StateEvent.StopCreating) --> localContext.input),
                        ),
                    ),
                )
            }
            .toSignal

        div(
            child <--adder,

            // Bind events
            globalEvents --> appContext.input,
            localContext.bind,
        )
