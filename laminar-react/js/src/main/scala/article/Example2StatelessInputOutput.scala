package article

import com.raquo.laminar.api.L.{*, given}
import common.style.{Flex, customButton, customInput}
import org.scalajs.dom

import scala.util.Random

object StatelessInputOutput:
    final case class Props(
        value: String,
    )

    enum Event:
        case ChangeValue(newValue: String)
        case SubmitValue(value: String)

    def apply(in: Signal[Props], out: Sink[Event]): HtmlElement =
        Flex.row(
            customInput(
                value <-- in.map(_.value),
                onChange.mapToValue.map(Event.ChangeValue(_)) --> out,
            ),
            customButton(
                "Submit",
                onClick(_.withCurrentValueOf(in).map(t => Event.SubmitValue(t._2.value))) --> out,
            ),
        )

object Example2:
    def apply(): HtmlElement =
        val state = Var[String]("")
        val in = state.signal.map(StatelessInputOutput.Props(_))
        val events = EventBus[StatelessInputOutput.Event]()
        val changeEvents = events.events.collect:
            case StatelessInputOutput.Event.ChangeValue(v) => v
        val submitEvents = events.events.collect:
            case StatelessInputOutput.Event.SubmitValue(v) => v

        val wordStream = EventStream.periodic(3000).map(_ => Util.randomWord(5, 20))

        StatelessInputOutput(in, events.writer).amend(
            wordStream --> state.writer,
            changeEvents --> state.writer,
            submitEvents --> { word => dom.window.alert(word) }
        )

