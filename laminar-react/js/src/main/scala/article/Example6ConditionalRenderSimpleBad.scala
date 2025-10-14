package article

import com.raquo.laminar.api.L.{*, given}
import common.styles.{Flex, customButton, customInput, customOption, customSelect}
import org.scalajs.dom

import scala.util.Random

object ConditionalRenderSimpleBad:
    enum Choice:
        case One, Two

    final case class Props(choice: Choice, header: String, body: String)

    enum Event:
        case Choose1, Choose2

    def apply(in: Signal[Props], out: Sink[Event]): HtmlElement = {
        val headerElement: Signal[HtmlElement] = in.map(_.choice).map:
            case Choice.One => Flex.row(input(`type` := "checkbox"), "Choice 1")
            case Choice.Two => Flex.row(input(`type` := "checkbox"), "Choice 2")

        val choiceElement = in.map:
            case Props(Choice.One, header, body) =>
                StatelessInput(Val(StatelessInput.Props(header, body)))
            case Props(Choice.Two, header, body) =>
                Flex.row(
                    alignItems.start,
                    input(`type` := "checkbox"),
                    div(header),
                    div(
                        overflowX.auto,
                        body,
                    ),
                )

        def eventFromValue(choiceValue: String): Option[Event] =
            if choiceValue == Choice.One.toString then Some(Event.Choose1)
            else if choiceValue == Choice.Two.toString then Some(Event.Choose2)
            else None

        Flex.column(
            h3(child <-- headerElement),
            customSelect(
                customOption("Choice 1", value := Choice.One.toString),
                customOption("Choice 2", value := Choice.Two.toString),
                value <-- in.map(_.choice.toString),
                onChange.mapToValue
                    .map(eventFromValue)
                    .collect({ case Some(v) => v }) --> out,
            ),
            child <-- choiceElement,
        )
    }

object Example6:
    def apply(): HtmlElement =
        def randomText() = (Util.randomWord(5, 12), Util.randomPhrase(3, 12, 2, 50))

        val textSignal = EventStream.periodic(2000).map(_ => randomText()).toSignal(randomText())

        val choiceState = Var(ConditionalRenderSimpleBad.Choice.One)
        val in = choiceState.signal.combineWith(textSignal).map(t => ConditionalRenderSimpleBad.Props.apply.tupled(t))
        val out = choiceState.writer.contramap[ConditionalRenderSimpleBad.Event]:
            case ConditionalRenderSimpleBad.Event.Choose1 => ConditionalRenderSimpleBad.Choice.One
            case ConditionalRenderSimpleBad.Event.Choose2 => ConditionalRenderSimpleBad.Choice.Two

        ConditionalRenderSimpleBad(in, out)
