package article

import com.raquo.laminar.api.L.{*, given}
import util.routeSignal

object Example3RouteSignal:
    enum InputType:
        case SelectionInput(selectedValue: Option[Boolean])
        case TextInput(textValue: String)

    final case class State(inputType: Option[InputType])

    def apply(): HtmlElement =
        val state = Var(State(None))

        div(
            select(
                option(
                    "Choose input type",
                    value := "none",
                ),
                option(
                    "Select",
                    value := "select",
                ),
                option(
                    "Text",
                    value := "text",
                ),
                value := "none",
                onChange.mapToValue --> Observer[String] {
                    case "none" => state.set(State(None))
                    case "select" => state.set(State(Some(InputType.SelectionInput(None))))
                    case "text" => state.set(State(Some(InputType.TextInput(""))))
                },
            ),
            child <-- state.signal
                .routeSignal({
                    case State(None) => ()
                }) { _ =>
                    div()
                }
                .routeSignal({
                    case State(Some(selectState @ InputType.SelectionInput(_))) => selectState
                }) { selectStateSignal =>
                    select(
                        option(
                            "No selection",
                            value := "none",
                        ),
                        option(
                            "True",
                            value := "true",
                        ),
                        option(
                            "False",
                            value := "false",
                        ),
                        value <-- selectStateSignal.map {
                            case InputType.SelectionInput(None) => "none"
                            case InputType.SelectionInput(Some(true)) => "true"
                            case InputType.SelectionInput(Some(false)) => "false"
                        },
                        onChange.mapToValue --> Observer[String] {
                            case "none" => state.set(State(Some(InputType.SelectionInput(None))))
                            case "true" => state.set(State(Some(InputType.SelectionInput(Some(true)))))
                            case "false" => state.set(State(Some(InputType.SelectionInput(Some(false)))))
                        },
                    )
                }
                .routeSignal({
                    case State(Some(textState@InputType.TextInput(_))) => textState
                }) { textStateSignal =>
                    input(
                        value <-- textStateSignal.map(_.textValue),
                        onInput.mapToValue --> Observer[String](
                            newValue => state.set(State(Some(InputType.TextInput(newValue))))
                        ),
                    )
                }
                .result,
            padding := "20px",
        )
