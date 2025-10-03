package article

import com.raquo.laminar.api.L.{*, given}

object Example6HandleWithSignal:
    final case class Person(name: String, age: Int)

    object PersonDisplay:
        final case class Props(person: Person, index: Int)

        def apply(props: Signal[Props], handleRemove: Sink[Int]) =
            // Note this will have a kind of crazy type LockedEventKey[MouseEvent, MouseEvent, Int],
            // but you can think of it as equivalent to `EventProp[Int]`
            val onClickMappedToIndex = onClick.compose(
                _.withCurrentValueOf(props).map:
                    case (_, Props(_, index)) => index
            )

            div(
                text <-- props.map(p => s"${p.person.name}: ${p.person.age} years old"),
                button(
                    "Remove",
                    marginLeft := "10px",
                    onClickMappedToIndex --> handleRemove,
                )
            )

    final case class State(people: Vector[Person])

    def apply(): HtmlElement =
        val initialState = State(Vector(Person("John", 41), Person("Ian", 42), Person("Aaron", 30)))
        val state = Var(initialState)

        val removePersonAtIndex: Sink[Int] = state.updater[Int]:
            case (State(people), index) =>
                State(people.patch(index, Nil, 1)) // Scala's funny way of removing an element

        div(
            button("Reset", onClick --> state.updater[Any]((_, _) => initialState)),
            ul(
                marginTop := "10px",
                children <-- state.signal.map(_.people.zipWithIndex).split(_._1.name):
                    case (_, _, personSignal) =>
                        PersonDisplay(
                            personSignal.map(tuple => PersonDisplay.Props(tuple._1, tuple._2)),
                            removePersonAtIndex,
                        )
            ),
        )