package article

import com.raquo.laminar.api.L.{*, given}
import common.style.Flex

object StatelessInput:
    final case class Props(
        header: String,
        body: String,
    )

    def apply(in: Signal[Props]): HtmlElement =
        Flex.column(
            h2(
              text <-- in.map(_.header),
            ),
            p(
              text <-- in.map(_.body),
            )
        )

object Example1:
    def randomProps() =
        StatelessInput.Props(Util.randomWord(5, 12), Util.randomPhrase(3, 12, 2, 50))

    def apply(): HtmlElement =
        val in = EventStream.periodic(1500).map(_ => randomProps()).toSignal(randomProps())

        StatelessInput(in)

