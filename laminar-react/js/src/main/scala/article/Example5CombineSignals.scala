package article

import com.raquo.laminar.api.L.{*, given}

object Example5CombineSignals:
    object RepeatComponent:
        final case class State(numRepetitions: Int)

        final case class Props(word: String)

        def apply(props: Signal[Props]) =
            // State is reset on ever render
            val state = Var(State(1))

            // For things like click events we can make updaters that don't care about the event type
            val increaser = state.updater[Any]:
                case (State(n), _) => State(n + 1)

            val decreaser = state.updater[Any]:
                case (State(n), _) => State((n - 1).max(1))

            // Combine `word` from `props` with `numRepetitions` from `state.signal`
            val repeatedWord = props.combineWith(state.signal).map:
                case (Props(word), State(numReps)) => List.fill(numReps)(word).mkString(", ")

            div(
                h3(text <-- repeatedWord),
                button("Repeat more!", onClick --> increaser),
                button("Repeat less!", onClick --> decreaser),
            )

    def apply(): HtmlElement =
        // Top-level component that constructs the word and passes
        // it down to RepeatComponent
        final case class State(inputText: String)

        val state = Var(State(""))

        div(
            input(
                value <-- state.signal.map(_.inputText),
                onInput.mapToValue.map(txt => State(txt)) --> state.writer,
            ),
            RepeatComponent(state.signal.map(state => RepeatComponent.Props(state.inputText.strip()))),
        )
