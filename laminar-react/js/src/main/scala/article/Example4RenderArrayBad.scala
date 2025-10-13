package article

import com.raquo.laminar.api.L.{*, given}
import common.style.{Flex, customButton, customInput}
import org.scalajs.dom

import scala.util.Random

object RenderArrayBad:
    final case class Entry(label: String, text: String)

    final case class Props(
        header: String,
        body: List[Entry],
    )

    def apply(in: Signal[Props]): HtmlElement =
        val bodyEntries: Signal[List[HtmlElement]] = in.map: props =>
            props.body.map:
                case Entry(label, text) =>
                    li(
                        Flex.row(
                            input(`type` := "checkbox"),
                            div(label),
                            div(text)
                        )
                    )

        Flex.column(
            h3(
              text <-- in.map(_.header),
            ),
            ul(
                children <-- bodyEntries,
            )
        )

object Example4:
    def apply(): HtmlElement =
        val entries = EventStream.periodic(10000).flatMapSwitch: _ =>
            val currentEntries = Util.randomWords(3, 20, 3, 15).map: word =>
                RenderArrayBad.Entry(word, Util.randomPhrase(2, 6, 3, 8))
            EventStream.periodic(1500).map(_ => Random.shuffle(currentEntries))

        val headers = EventStream.periodic(4750).map(_ => Util.randomPhrase(2, 5, 4, 7))

        val in = headers.combineWith(entries).map(t => RenderArrayBad.Props(t._1, t._2))
            .toSignal(RenderArrayBad.Props("", Nil))

        RenderArrayBad(in)
