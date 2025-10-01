package article

import com.raquo.laminar.api.L.{*, given}

object Example4SplitArray:
    case class Person(name: String, age: Int)

    def apply(): HtmlElement =
        val state = Var(Vector(Person("John", 41), Person("Angie", 52), Person("Derek", 28)))

        ul(
            children <-- state.signal.split(_.name) { (_, _, signal) =>
                li(text <-- signal.map(v => s"${v.name}: ${v.age} years old").distinct)
            },
            ul(button("reverse", onClick --> Observer[Any](_ => state.update(_.reverse))))
        )
