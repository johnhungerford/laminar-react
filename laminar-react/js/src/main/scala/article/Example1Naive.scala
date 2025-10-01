package article

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.{HTMLDivElement, document}

object Example1Naive:
    enum InputType:
        case SelectionInput(selectedValue: Option[Boolean])
        case TextInput(textValue: String)

    final case class State(inputType: Option[InputType])

    def apply(): HtmlElement =
        val state = Var(State(None))

        div(
            h1("Example"),
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
            child <-- state.signal.map {
                case State(None) => div()
                case State(Some(InputType.SelectionInput(selection))) =>
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
                        value := (selection match {
                            case None => "none"
                            case Some(true) => "true"
                            case Some(false) => "false"
                        }),
                        onChange.mapToValue --> Observer[String] {
                            case "none" => state.set(State(Some(InputType.SelectionInput(None))))
                            case "true" => state.set(State(Some(InputType.SelectionInput(Some(true)))))
                            case "false" => state.set(State(Some(InputType.SelectionInput(Some(false)))))
                        },
                    )
                case State(Some(InputType.TextInput(textValue))) =>
                    input(
                        value := textValue,
                        onInput.mapToValue --> Observer[String](newValue => state.set(State(Some(InputType.TextInput(newValue))))),
                    )

            },
            padding := "20px"
        )

