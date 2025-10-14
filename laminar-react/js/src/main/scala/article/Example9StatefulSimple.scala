package article

import com.raquo.laminar.api.L.{*, given}
import common.styles.{Flex, customButton, customInput, customOption, customSelect}
import org.scalajs.dom

import scala.util.Random

object StatefulSimple:
    final case class Props(
        header: String,
        body: String,
    )

    val collapsedState: Var[Boolean] = Var(false)

    def apply(in: Signal[Props]): HtmlElement =
        Flex.column(
            h2(
                text <-- in.map(_.header),
            ),
            Flex.row(
                div("Hide text: "),
                input(
                    `type` := "checkbox",
                    checked <-- collapsedState.signal,
                    onChange.mapToChecked --> collapsedState.writer,
                ),
            ),
            child <-- collapsedState.signal.distinct.map:
                case true => emptyNode
                case false =>
                    div(
                        text <-- in.map(_.body),
                    )
        )

object Example9:
    import StatefulSimple.Props

    def randomProps() =
        Props(Util.randomWord(5, 12), Util.randomPhrase(3, 12, 2, 50))

    def apply(): HtmlElement =
        val in = EventStream.periodic(1500).map(_ => randomProps()).toSignal(randomProps())

        StatefulSimple(in)
