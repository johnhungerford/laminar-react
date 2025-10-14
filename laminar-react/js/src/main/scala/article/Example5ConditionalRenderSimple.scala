package article

import com.raquo.laminar.api.L.{*, given}
import common.styles.{Flex, customButton, customInput, customOption, customSelect}
import org.scalajs.dom

import scala.util.Random

object ConditionalRenderSimple:
    enum Choice:
        case One, Two

    final case class Props(choice: Choice, header: String, body: String)

    enum Event:
        case Choose1, Choose2

    def apply(in: Signal[Props], out: Sink[Event]): HtmlElement = {
        // .distinct is an easy way to render conditionally in simple cases
        // as it ensures the element will be rerendered only when the value
        // *changes*.
        val headerElement: Signal[HtmlElement] = in.map(_.choice).distinct.map:
            case Choice.One => Flex.row(input(`type` := "checkbox"), "Choice 1")
            case Choice.Two => Flex.row(input(`type` := "checkbox"), "Choice 2")

        // .splitOne is another way to render conditionally. It is slightly
        // more powerful than .distinct by allowing you to render
        // based on both the value of the key and the *initial* value of the
        // signal upon rendering
        val choiceElement = in.splitOne(_.choice):
            case (Choice.One, _, signal) =>
                StatelessInput(signal.map(p => StatelessInput.Props(p.header, p.body)))
            case (Choice.Two, _, signal) =>
                Flex.row(
                    alignItems.start,
                    input(`type` := "checkbox"),
                    div(text <-- signal.map(_.header)),
                    div(
                        overflowX.auto,
                        text <-- signal.map(_.body),
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

object Example5:
    def apply(): HtmlElement =
        def randomText() = (Util.randomWord(5, 12), Util.randomPhrase(3, 12, 2, 50))

        val textSignal = EventStream.periodic(2000).map(_ => randomText()).toSignal(randomText())

        val choiceState = Var(ConditionalRenderSimple.Choice.One)
        val in = choiceState.signal.combineWith(textSignal).map(t => ConditionalRenderSimple.Props.apply.tupled(t))
        val out = choiceState.writer.contramap[ConditionalRenderSimple.Event]:
            case ConditionalRenderSimple.Event.Choose1 => ConditionalRenderSimple.Choice.One
            case ConditionalRenderSimple.Event.Choose2 => ConditionalRenderSimple.Choice.Two

        ConditionalRenderSimple(in, out)
