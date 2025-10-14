package article

import com.raquo.laminar.api.L.{*, given}
import common.styles.{Flex, customButton, customInput, customOption, customSelect}
import org.scalajs.dom

import scala.util.Random

object ConditionalRenderComplex:
    enum Props:
        case Choice1(header: String, body: String)
        case Choice2(header: String, body: List[(String, String)])

    enum Event:
        case Choose1, Choose2

    def apply(in: Signal[Props], out: Sink[Event]): HtmlElement = {
        val choiceElement = in
            .splitMatchOne
            // First variant: match case, transform, render
            .handleCase({
                case Props.Choice1(header, body) => StatelessInput.Props(header, body)
            }) { (_, propsSignal) =>
                StatelessInput(propsSignal)
            }
            // Second variant: match type, render
            .handleType[Props.Choice2] { (_, propsSignal) =>
                val renderArrayIn = propsSignal
                    .map(p => RenderArray.Props(p.header, p.body.map(RenderArray.Entry.apply.tupled)))
                RenderArray(renderArrayIn)
            }
            .toSignal

        val selectValue = in.map:
            case _: Props.Choice1 => "1"
            case _: Props.Choice2 => "2"

        def eventFromValue(value: String): Option[Event] =
            if value == "1" then Some(Event.Choose1)
            else if value == "2" then Some(Event.Choose2)
            else None

        Flex.column(
            customSelect(
                customOption("Choice 1", value := "1"),
                customOption("Choice 2", value := "2"),
                value <-- selectValue,
                onChange.mapToValue
                    .map(eventFromValue)
                    .collect( { case Some(i) => i }) --> out,
            ),
            child <-- choiceElement
        )
    }

object Example7:
    import ConditionalRenderComplex.{Props, Event}

    def apply(): HtmlElement =
        def randomLabel() = Util.randomWord(5, 12)
        def randomSentence() = Util.randomPhrase(3, 12, 2, 50)
        def randomEntries() = Util.randomWords(2, 30, 3, 12).map((_, Util.randomPhrase(3, 12, 4, 8)))

        val headerSignal = EventStream.periodic(3500).map(_ => randomLabel()).toSignal(randomLabel())
        val choice1Signal = EventStream.periodic(1500).map(_ => randomSentence()).toSignal(randomSentence())
        val choice2Signal = EventStream.periodic(12000).flatMapSwitch { _ =>
            val entries = randomEntries()
            EventStream.periodic(1500).map(_ => Random.shuffle(entries))
        }.toSignal(randomEntries())

        val state = Var(Event.Choose1)

        val out = state.writer

        val in = state.signal.flatMapSwitch:
            case Event.Choose1 =>
                headerSignal.combineWith(choice1Signal).map:
                    case (header, sentence) => Props.Choice1(header, sentence)
            case Event.Choose2 =>
                headerSignal.combineWith(choice2Signal).map:
                    case (header, entries) => Props.Choice2(header, entries)

        ConditionalRenderComplex(in, out)
