package todo

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import org.scalajs.dom.HTMLInputElement
import todo.model.{AppEvent, ToDo, ToDoList}
import common.StateContext
import common.styles.{Flex, Icon, customButton, customInput}


/** Create a new to do item for a given list. Displays a prompt button;
  * when clicked, displays a text input for the new to-do label and details
  */
object CreateToDoComponent:
    final case class Props(list: ToDoList, existingLabels: Set[String])

    private enum State:
        case Initial
        case Adding(labelText: String, detailsText: String)

        def reduce(event: StateEvent): State = event match
            case StateEvent.StartAdding => Adding("", "")
            case StateEvent.StopAdding => Initial
            case StateEvent.SetLabel(value) => this match
                case State.Adding(_, detailsText) => State.Adding(value, detailsText)
                case _ => this
            case StateEvent.SetDetails(value) => this match
                case State.Adding(labelText, _) => State.Adding(labelText, value)
                case _ => this
            case StateEvent.Add => Initial

    private enum StateEvent:
        case StartAdding, StopAdding, Add
        case SetLabel(value: String)
        case SetDetails(value: String)

    def apply(in: Signal[Props])(using appContext: AppContext): Signal[HtmlElement] = {
        // Rerender once for each new list
        in.splitOne(_.list): (_, _, _) =>
            // Initialize local state. Needs to be bound to the element (see below)
            val localContext = StateContext[State, StateEvent](
                State.Initial,
                (state, event) => state.reduce(event)
            )

            // Collect local events that should trigger global events, and transform them
            // accordingly. Needs to be bound to global store input (see below)
            val globalEvents = localContext.events.withCurrentValueOf(in, localContext.state).collect:
                case (StateEvent.Add, Props(list, _), State.Adding(label, details)) =>
                    val detailsOpt = if details.strip().isEmpty then None else Some(details.strip())
                    AppEvent.NewToDo(list, label.strip(), detailsOpt)

            val innerElement: Signal[HtmlElement] = localContext.state.combineWith(in)
                .splitMatchOne
                // Initial state: render a button to add a new todo
                .handleCase({ case (State.Initial, _) => () }) { (_, _) =>
                    div(
                        customButton(
                            Flex.row(gap := "5px", Icon.plus(color.white), div("Add todo")),
                            onClick.mapTo(StateEvent.StartAdding) --> localContext.input,
                        ),
                    )
                }
                // Once button above has been pressed, render a form for
                // creating a new to-do item
                // Note you need type annotations on your match expression due
                // underlying macro implementation
                .handleCase({ case (st: State.Adding, pr: Props) => (st, pr) }) { (_, signal) =>
                    val addingSignal = signal.map(_._1: State.Adding)

                    val addDisabled = signal.map:
                        case (State.Adding(label, _), Props(_, existingLabels)) =>
                            label.strip().isEmpty || existingLabels.contains(label.strip())

                    form(
                        onSubmit.preventDefault.mapTo(StateEvent.Add)
                            --> localContext.input,
                        Flex.column(
                            Flex.row(
                                "Label",
                                customInput(
                                    value <-- addingSignal.map(_.labelText),
                                    onInput.map(v => StateEvent.SetLabel(v.currentTarget.asInstanceOf[HTMLInputElement].value))
                                        --> localContext.input,
                                    onMountFocus
                                ),
                            ),
                            Flex.row(
                                "Details",
                                customInput(
                                    value <-- addingSignal.map(_.detailsText),
                                    onInput.map(v => StateEvent.SetDetails(v.currentTarget.asInstanceOf[HTMLInputElement].value))
                                        --> localContext.input,
                                ),
                            ),
                            Flex.row(
                                customButton(
                                    "Add",
                                    `type` := "submit",
                                    disabled <-- addDisabled,
                                ),
                                customButton(
                                    "Cancel",
                                    `type` := "button",
                                    onClick.mapTo(StateEvent.StopAdding) --> localContext.input,
                                ),
                            ),
                        ),
                    )
                }
                .toSignal

            div(
                child <-- innerElement,

                // Bind events
                localContext.bind,
                globalEvents --> appContext.input,
            )
    }
