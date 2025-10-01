package article

import com.raquo.laminar.api.L.{*, given}

object Example2SplitOne:
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
            child <-- state.signal.splitOne({
                case State(None) => 0
                case State(Some(InputType.SelectionInput(_))) => 1
                case State(Some(InputType.TextInput(_))) => 2
            }) {
                case (_, State(None), _) => div()
                case (_, State(Some(InputType.SelectionInput(_))), signal) =>
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
                        value <-- (signal.map {
                            case State(Some(InputType.SelectionInput(Some(true)))) => "true"
                            case State(Some(InputType.SelectionInput(Some(false)))) => "false"
                            case _ => "none"
                        }),
                        onChange.mapToValue --> Observer[String] {
                            case "none" => state.set(State(Some(InputType.SelectionInput(None))))
                            case "true" => state.set(State(Some(InputType.SelectionInput(Some(true)))))
                            case "false" => state.set(State(Some(InputType.SelectionInput(Some(false)))))
                        },
                    )
                case (_, State(Some(InputType.TextInput(_))), signal) =>
                    input(
                        value <-- signal.map {
                            case State(Some(InputType.TextInput(textValue))) => textValue
                            case _ => ""
                        },
                        onInput.mapToValue --> Observer[String](
                            newValue => state.set(State(Some(InputType.TextInput(newValue))))
                        ),
                    )
            },
        )
