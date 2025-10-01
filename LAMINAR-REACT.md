
# Scala.js for React developers

> Should I read this?
> 
Do you have front-end experience with React and some familiarity with Scala? Are you interested in trying frontend development Scala.js? If so, this could be helpful for you.

> Do I need to read the whole thing?
> 
If you are already familiar with Laminar skip to ["The react model in Laminar"](#the-react-model-in-laminar) or ["Simpler splitting"](#simpler-splitting) to learn about how to render elements conditionally in an ergonomic way. Also feel free to just skip to the code, which can be found here: [github.com/johnhungerford/laminar-react].

## Introduction

It is no secret that Javascript is a language unsuitable for the scale it has reached. TypeScript too, while a huge improvement on JS, sticks too close to Javascript to take advantage of many features that are gaining traction in modern programming languages. It was therefore a great relief to me as a Scala developer when a few years ago I was tasked with building a simple front-end application for my team that I had the option of using Scala.js as a browser language. I was not disappointed in the choice, and have consistently found it to be a far more productive for web development than its alternatives. Since this time, moreover, the trade-offs this choice demands -- which were substantial at first -- have steadily diminished. Bundle sizes have consistently shrunk (and bundle-splitting is now supported), compilation times have shortened, and tooling has improved drastically. By this point, Scala.js has the footprint of a framework like React and is absolutely useable as a production solution (depending on requirements).

When I first started using Scala.js, I had already used React on some other projects, so I decided to use scalajs-react, a Scala wrapper around the React framework. This would allow me to leverage the vast ecosystem of React components while using an archicture I was familiar with. While this worked fairly well, I was persistently nagged by the following thoughts:
1. Scala is great for designing abstractions -- especially for things like what React is doing, namely, modeling state updates via immutable data. It seems crazy to be relying on a JS solution for what Scala really shines at.
2. Scala's excellence in abstraction makes relying on community solutions less important. It was so easy to design my own windowed list in Scala.js, I skipped react-virtualized. Writing façades for the JS components often was about the same amount of work rolling my own. If I could only find a good Scala-native library that could deal with *style* (like material-ui), I thought, there would be no need for the ecosystem.
3. Since Scala.js already presents a framework's worth of overhead, it seems crazy to rely on another framework underneath it. For Scala.js ever to become a serious contender, it would have to have a more light-weight abstraction for DOM rendering.

For these reasons I was really excited when Laminar was released -- the first Scala-only library for designing browser applications with real traction. Not only did Laminar mean React was not required, but the introduction of [web components](https://developer.mozilla.org/en-US/docs/Web/API/Web_components) has made it possible to use JS libraries without needing a JS framework. Some popular web-component libraries already have Laminar wrappers (I recommend [web awesome](https://github.com/nguyenyou/webawesome-laminar)), so I would not have to worry about the styling!

In my most recent opportunity to do some web-development in Scala, therefore, I decided to use Laminar and ditch React entirely. This experience eventually led to great results, but I admit it took me a while to work out how to get laminar to do what I wanted. It is a fairly unopinionated library, and there a lot of ways to go wrong using it. It also took me a while to come to grips with the implications of its significant differences with React. After some effort, however, I worked out a way to get Laminar working for me better than scalajs-react ever did. Moreover, I did so without having to totally through out the mental model I had inherited from React.

In this article I will share with you what I learned from this experience. I will provide an introduction to Laminar, show how Laminar departs from React and the challenges that this presents, and the show you my solution to these challenges. At the end, I will sketch a suggested application architecture. For a more complete example application, you can look at the [accompanying github repository](), where you can also find (and run!) all the code in this article.

## Laminar

Laminar is the most widely adopted web development library native to Scala.js. It allows user to construct DOM elements via an intuitive and flexible declaritive API, and is shipped with its own reactive data framework (Airstream) for managing DOM events and updates. Here is what a simple Laminar application looks like:

```scala
import com.raquo.Laminar.api.L.{*, given}

val textState: Var[String] = Var("Name") // Reactive "state" for the text to display

val element = div(
	h1("Example"),
    text <-- textState.signal, // The element text will be updated with the state
    input(
    	value <-- textState.signal, // The input text will be set by the state
    	onInput.mapToValue --> textState.writer, // Updates to the text will propagate to the state
    ),
    padding := "20px", // Properties can also be set with static values
)

// Render the element in the DOM
render(
	dom.document.querySelector("#app"),
	element,
)
```

As the above example illustrates, elements are constructed declaratively using functions named after their corresponding tags. These functions accept child elements, property settings, and event handlers as arguments. Property settings are defined using `:=` for static values and `<--` for reactive data. Setting properties using a reactive value like `Signal` or `EventStream` will ensure that the property is updated with each new value. Event handlers are constructed using `-->`, which pipes an event listener (e.g., `onClick` or `onChange`) into a reactive `Observer`.

Scala's powerful type system and expressive functional syntax makes it easy to pipe the data between reactive elements of various types. For instance, in the above example the reactive `Var` `nameState` exposes an `Observer` via `textState.writer` which accepts `String` inputs, whereas the event listener `onClick` represents a stream of `MouseEvent`s. The method `mapToValue` transforms this to a stream `String`s by extracting the input value from each event (it is equivalent to `textState.map(_.currentTarget.value))`). Now that it matches the observer's type, it can be bound to it using `-->`.

While these features can be used to construct elements and update their properties in response to DOM events, they do not yet indicate how to render elements conditionally. What if you need to render an `input` or a `select` element depending on your application state? To accomplish this, Laminar allows you to bind streams or signals of elements to elements using the `child` (for single elements) and `children` (for collections of elements) properties. This can be used to render a signal conditionally:

```scala
import com.raquo.Laminar.api.L.{*, given}

enum InputType:
    case SelectionInput(selectedValue: Option[Boolean])
    case TextInput(textValue: String)

final case class State(inputType: Option[InputType])

val state = Var(State(None))

val element = div(
    select(
        option(
            "Choose input type",
            value := "none",
        ),
        option(
            "Select",
            value := "select",
        ),
        option(
            "Text",
            value := "text",
        ),
        value := "none",
        onChange.mapToValue --> Observer[String] {
            case "none" => state.set(State(None))
            case "select" => state.set(State(Some(InputType.SelectionInput(None))))
            case "text" => state.set(State(Some(InputType.TextInput(""))))
        },
    ),
    child <-- state.signal.map {
        case State(None) => div()
        case State(Some(InputType.SelectionInput(selection))) =>
            select(
                option(
                    "No selection",
                    value := "none",
                ),
                option(
                    "True",
                    value := "true",
                ),
                option(
                    "False",
                    value := "false",
                ),
                value := (selection match {
                    case None => "none"
                    case Some(true) => "true"
                    case Some(false) => "false"
                }),
                onChange.mapToValue --> Observer[String] {
                    case "none" => state.set(State(Some(InputType.SelectionInput(None))))
                    case "true" => state.set(State(Some(InputType.SelectionInput(Some(true)))))
                    case "false" => state.set(State(Some(InputType.SelectionInput(Some(false)))))
                },
            )
        case State(Some(InputType.TextInput(textValue))) =>
            input(
                value := textValue,
                onInput.mapToValue --> Observer[String](newValue => state.set(State(Some(InputType.TextInput(newValue))))),
            )

    },
)
```

In the above example, the first `select` element chooses the input type, and the next `child` "receiver" is bound to an element rendered from the current state. (Scala's support for pattern matching makes conditional rendering from state much more elegant than in Javascript frameworks.)

## Laminar vs React

The above example seems to indicate a clean correspondance between Laminar's reactive model and React's functional component model. All of the pieces are in place to translate a react application to Laminar: store the state in `Var`, map the state's `signal` to conditionally render each component, and pipe event listeners to `Observer` callbacks that update the state.

This apparent similarity overlooks an important feature of react that is missing from Laminar. What's missing will become clear once we run the example. Rendered in the browser, everything works fine until we select `Text` from the input selector. This will render a text input, into which we can begin typing some text. The first character we type appears in the input box, but the next characters do not! For some reason, after we type the first character the input element loses focus. We can click on the input to try typing again, but the same thing happens. Every time we type a character we need to click the input box again. Why is this?

The answer is simple: every time we change the input, it updates the state and *the entire element is rerendered*. Each time this happens, the focus is lost. While this particular problem could be fixed by configured the input to be focused on each render, the bigger issue is that elements are being rerendered when they shouldn't be. In fact, every single element within the reactive `child` will be rerendered with every change of state. This is going to produce a lot of different issues as our application grows in complexity. How can this be avoided?

In react, this is a problem we don't have to think about very often. The reason is that react components do not render elements directly. Instead, they construct what is called a "virtual DOM" -- a parallel data-only image of the DOM. Every change of state fully regenerates the virtual DOM, but instead of rendering the whole thing, react compares it with its previous version, and only updates those parts of it that have changed. This approach is very powerful -- it means we rarely have to think about when to render an element or not.

While powerful, the React approach is also a bit overkill. In order to make reliable rendering decisions, it needs to maintain an entire reconstruction of the DOM in memory. It also needs to undertake an elaborate "diffing" process on each state update to decide what gets rendered and what doesn't. The code and memory overhead needed for this are not negligible.

Laminar's reactive data is a much more light-weight mechanism for propagating updates. The cost, however, is more mental energy devoted to optimizing rendering. This cost can be very high for newcomers to Laminar -- especially for those coming from react. There are different ways to design around the rendering problem, and it is easy to come up with bad and confusing solutions. These solutions can often give up a lot of the important benefits of the react mental model. It is not necessary to abandon the react model, however! The remainder of this article will try to show how react's mental model can be optimally approximated within Laminar so that the developer can gain the benefits of Laminar's reactive approach along with benefits of react's architectural simplicity.

## The react model in Laminar

To begin with, let's sketch our requirements. What we want is to be able to model our application as a function from state to view. We do not want to be hand-wiring the various updates, and we do not want to have to think about when each update will actually be made. We want that to be generally taken care of for us.

We can assume that state will always be propagated throughout the application in the form of a `Signal`. The basic problem, then, is how to "choose" between cases of the data delivered in a signal, and render based on those cases without re-rendering -- while also having access to a signal of *only those cases of the data* to render with.

Laminar has part of a solution out-of-the-box with the `splitOne` method on a signal. `splitOne` requires you to (1) generate a "key" for each value produced by your signal, and (2) render an element using the original signal. The rendering function provided in (2) will only be called once throughout any series of signal updates in which the key generated by (1) has not changed. This addresses the basic rendering problem. We can update our original application as follows:


```scala
val element = div(
    select(
        option(
            "Choose input type",
            value := "none",
        ),
        option(
            "Select",
            value := "select",
        ),
        option(
            "Text",
            value := "text",
        ),
        value := "none",
        onChange.mapToValue --> Observer[String] {
            case "none" => state.set(State(None))
            case "select" => state.set(State(Some(InputType.SelectionInput(None))))
            case "text" => state.set(State(Some(InputType.TextInput(""))))
        },
    ),
    child <-- state.signal.splitOne({
        case State(None) => 0
        case State(Some(InputType.SelectionInput(_))) => 1
        case State(Some(InputType.TextInput(_))) => 2
    }) {
        case (_, State(None), _) => div()
        case (_, State(Some(InputType.SelectionInput(_))), signal) =>
            select(
                option(
                    "No selection",
                    value := "none",
                ),
                option(
                    "True",
                    value := "true",
                ),
                option(
                    "False",
                    value := "false",
                ),
                value <-- (signal.map {
                    case State(Some(InputType.SelectionInput(Some(true)))) => "true"
                    case State(Some(InputType.SelectionInput(Some(false)))) => "false"
                    case _ => "none"
                }),
                onChange.mapToValue --> Observer[String] {
                    case "none" => state.set(State(Some(InputType.SelectionInput(None))))
                    case "true" => state.set(State(Some(InputType.SelectionInput(Some(true)))))
                    case "false" => state.set(State(Some(InputType.SelectionInput(Some(false)))))
                },
            )
        case (_, State(Some(InputType.TextInput(_))), signal) =>
            input(
                value <-- signal.map {
                    case State(Some(InputType.TextInput(textValue))) => textValue
                    case _ => ""
                },
                onInput.mapToValue --> Observer[String](
                    newValue => state.set(State(Some(InputType.TextInput(newValue))))
                ),
            )
    },
)
```

This updated version now generates an integer key for each of the three match cases of the state (0, 1, or 2), ensuring that the subsequent render function will not be re-executed until the state leaves that case. The render function then matches again on the *original* value of the state when a given key value is first encountered (the render function for `splitOne` takes three parameters: the key, the original signal value, and the signal itself) to determine which way to render.

While this does solve the basic rendering problem, which we can confirm running the updates in our browser, we immediately see some issues with the code. It requires a fair amount of boilerplate and includes some troubling redundancy. For instance, we have to match on the *full state* in *four* places! First, when we generate the key; second, for conditional rendering; third, when mapping the state signal to the `select` value; and fourth, when mapping the state signal to the text input value. It's fair to say this does not enter the ball-park of React's conceptual simplicity.

## Simpler splitting

How can this be better? What we'd really like to do is to narrow our state down to the individual cases we'd like to render, and then render each of those cases using a signal of that narrowed state, ignoring state updates that are not relevant to the current case. To accomplish this using the existing `splitOne` method, we'll have to provide a separate matching function and rendering function for each case. We can accumulate these cases, which we'll call "routes" and then generate a signal at the end using `splitOne`. The API should look like this:

```scala
enum State:
	case Case1(int: Int)
	case Case2(str: String)

val routedSignal: Signal[HtmlElement] = state.signal
	.routeSignal({ case State.Case1(value) => value }) { (case1Signal: Signal[Int]) =>
		div("Case 1!", height <-- case1Signal)
	}
	.routeSignal({ case State.Case2(value) => value }) { (case2Signal: Signal[String]) =>
		div(text <-- case1Signal.map(txt => s"Case 2!: $txt"))
	}
	.result
```

This approach looks a lot closer to a simple pattern match, while still propagating state in a signal. Now, however, the propagated signal is narrowed down to the subtype as determined by the matching function for each route.

Implementing this API is not too difficult. Each route can be modeled as a `SignalRoute` consisting of a partial function "select" and a render function "map" and these `SignalRoute`s can be collected in `SignalRouter` which can generate the resulting signal by composing all the "select"s into a single key function and composing all the "map"s (each using its corresponding "select" function to narrow down the signal) into a single render function. The full implementation is as follows:

```scala

case class SignalRoute[A, B, +C](select: PartialFunction[A, B], map: Signal[B] => C)

sealed case class SignalRouter[A, +C](signal: Signal[A], routes: Vector[SignalRoute[A, ?, C]]):
    private def selectPartial[B](select: PartialFunction[A, Any], result: B): PartialFunction[A, B] = {
        case (a: A) if select.isDefinedAt(a) => result
    }

    private def splitFn[C1 >: C](default: Signal[A] => C1): A => (Int | Unit) =
        val indexedRoutes = routes.zipWithIndex
        indexedRoutes.headOption match
            case None => _ => ()
            case Some((SignalRoute(select, _), i)) =>
                indexedRoutes.tail.foldLeft(selectPartial(select, i)) {
                    case (pf, (SignalRoute(select, _), i)) =>
                        pf.orElse(selectPartial(select, i))
                }.orElse({ case _ => () })

    def result[C1 >: C](default: Signal[A] => C1): Signal[C1] = signal.splitOne(splitFn(default)):
        case ((), _, splitSignal) =>
            default(splitSignal)
        case (i: Int, _, splitSignal) =>
            val route = routes(i)
            val typedSignal = splitSignal.map(route.select).distinct
            route.map(typedSignal)

    def result[C1 >: C](default: C1): Signal[C1] = result[C1](_ => default)

    def result[C1 >: C]: Signal[C1] = result(_ => throw IllegalStateException("Unmatched signal route!"))

    def routeSignal[B](select: PartialFunction[A, B])[C1 >: C](map: Signal[B] => C1): SignalRouter[A, C1] =
        copy[A, C1](routes = routes.appended(SignalRoute[A, B, C1](select, map)))

// Provide an extension method to start routing directly from a signal
extension [A](signal: Signal[A])
    def routeSignal[B](select: PartialFunction[A, B])[C](map: Signal[B] => C): SignalRouter[A, C] =
        SignalRouter(signal, Vector(SignalRoute(select, map)))
```

Now that we have a more straightforward way of routing our signal, let's update our original application:

```scala
val element = div(
    select(
        option(
            "Choose input type",
            value := "none",
        ),
        option(
            "Select",
            value := "select",
        ),
        option(
            "Text",
            value := "text",
        ),
        value := "none",
        onChange.mapToValue --> Observer[String] {
            case "none" => state.set(State(None))
            case "select" => state.set(State(Some(InputType.SelectionInput(None))))
            case "text" => state.set(State(Some(InputType.TextInput(""))))
        },
    ),
    child <-- state.signal
        .routeSignal({
            case State(None) => ()
        }) { _ =>
            div()
        }
        .routeSignal({
            case State(Some(selectState @ InputType.SelectionInput(_))) => selectState
        }) { selectStateSignal =>
            select(
                option(
                    "No selection",
                    value := "none",
                ),
                option(
                    "True",
                    value := "true",
                ),
                option(
                    "False",
                    value := "false",
                ),
                value <-- selectStateSignal.map {
                    case InputType.SelectionInput(None) => "none"
                    case InputType.SelectionInput(Some(true)) => "true"
                    case InputType.SelectionInput(Some(false)) => "false"
                },
                onChange.mapToValue --> Observer[String] {
                    case "none" => state.set(State(Some(InputType.SelectionInput(None))))
                    case "true" => state.set(State(Some(InputType.SelectionInput(Some(true)))))
                    case "false" => state.set(State(Some(InputType.SelectionInput(Some(false)))))
                },
            )
        }
        .routeSignal({
            case State(Some(textState@InputType.TextInput(_))) => textState
        }) { textStateSignal =>
            input(
                value <-- textStateSignal.map(_.textValue),
                onInput.mapToValue --> Observer[String](
                    newValue => state.set(State(Some(InputType.TextInput(newValue))))
                ),
            )
        }
        .result,
    padding := "20px",
)
```

This new approach solves the rendering problem while keeping as closely as possible to the React approach. It is still not perfect. For instance, breaking up the match cases into separate routes makes it impossible for the compiler to validate the completeness of our matches. If we add another case to our `State` enum, the compiler will not warn us, and the application will fail with a `MatchError` as soon as that new case is reached. If we accidentally make one match case too broad, the compiler will not warn us of "unreachable" cases. Despite this inconvenience, it is the best approximation of React-style conditional rendering I have been able to achieve using Laminar.

## Other tricks

### Update on changes only

While our `.routeSignal` utility solves the main rendering problem, there is still the issue that any properties we have bound to our state signal will be updated each time the signal emits a new value, even if the value hasn't changed. This can lead to a fair amount of unnecessary updates. To avoid this problem, Laminar provides a `distinct` method on `Signal` to generate a new signal that only emits values when the original signal emits a value different from the last one. This is sort of the Laminar equivalent of React's "diffing" of the virtual DOM.

### Reactive element arrays

Though `.routeSignal` solves the rendering issue for single elements, anyone who has used React is familiar with a similar problem for arrays of elements. Any time a sequence of elements is generated from a collection via `map`, some step must be taken to ensure that each mapped element is not rerendered each time. To address this issue, we can use Laminar's own `.split` method without needing to customize it at all.

`.split` works a lot like the React approach, which is to assign a key to each element in the array so that React knows what to compare it to from the previous rendering. `.split`, like `.splitOne`, requires you to map the signal to a key prior to rendering. The rendering function will not be re-executed for any elemented emitted if its key was generated in the last rendering -- even if it is at a different place in the array. This is what the `.split` looks like in a Laminar application:

```scala
case class Person(name: String, age: Int)

val state = Var(Vector(Person("John", 41), Person("Angie", 52), Person("Derek", 28), Person("Sharon", 78)))

ul(
    children <-- state.signal.split(_.name) { (_, _, signal) =>
        li(
            text <-- signal.map(v => s"${v.name}: ${v.age} years old").distinct,
            input(`type` := "checkbox", marginLeft := "10px"),
        )
    },
    ul(
        button("reverse", onClick --> Observer[Any](_ => state.update(_.reverse))),
        marginTop := "15px",
    )
)
```

In the above example, rearranging the list of people will not rerender any of the individual list items. For instance, if you select any of the checkboxes and then click the "reverse" button, the selection will remain (since the checkboxes state is not controlled, rerendering would clear the selection).

## Application architecture

Now that we have all of the components we need to render state in a quasi-functional manner while avoiding unnecessary re-rendering, let's sketch what a complete React-like application would look like in Laminar.

### State and events

While React supports a number of different state management paradigms, Laminar is particular well-suited for *event-driven architecture*. This is because the reactive architecture is designed primarily for data rather than callbacks. Thus, instead of binding `onClick` to an observer that updates state via callback, it is cleaner to *map* the `onClick` DOM event to a custom *event* and bind the updated listener to an observer that processes these custom events.

The result is to construct your top-level application state in a manner similar to redux: define a "store" (in our case we'll just make this a `Var`) and a "reducer" (a function that updates state based on events) and then wire them together:

```scala
enum WindowState:
	case Window1State(...)
	case Window2State(...)

final case class GlobalState(
	windowState: WindowState
	...
)

enum GlobalEvent:
	case ChooseWindow1, ChooseWindow2,
	...

object Store:
	private val stateVar = Var(GlobalState(???)) // initial value

	val eventSink: Sink[GlobalEvent] = stateVar.updater[GlobalEvent]:
		case (state, event) => ??? // This is your reducer: generate new state

	val state: Signal[State] = stateVar.signal
```

The `Store` object above exposes two public members that can be used for your application: `state`, which provides the current value of the state, and `eventSink` that `GlobalEvent`s can be wired into from reactive event listeners.

### Component hierarchy

Now let's sketch what a top level component might look like:

```scala
object AppComponent:
	def apply(): HtmlElement =
		val windowState = Store.state.map(_.windowState).distinct

		div(
			Menu(windowState),
			child <-- windowState
				.routeSignal({ case st: WindowState.Window1State => st })(w1Signal => Window1(w1Signal))
				.routeSignal({ case st: WindowState.Window2State => st })(w2Signal => Window2(w2Signal)),
				.result,
		)
```

The top level component narrows the global state down to `WindowState`, filters out extraneous updates (`.distinct`), and passes this first to the menu component. It then conditionally renders the selected window, passing the further narrowed state to the appropriate window component.

These components may not need to access the global state at all, but can simply use the signal passed to them:

```scala
object Menu:
	def apply(input: Signal[WindowState]): HtmlElement =
		ul(
			li(
				"Window 1",
				cursor.pointer,
				onClick.mapTo(GlobalEvent.ChooseWindow1) --> Store.eventSink,
				backgroundColor <-- input.map: // Highlight when selected
					case _: WindowState.Window1State => "lightblue"
					case _ => "inherit",
			),
			li(
				"Window 2",
				cursor.pointer,
				onClick.mapTo(GlobalEvent.ChooseWindow1) --> Store.eventSink,
				backgroundColor <-- input.map: // Highlight when selected
					case _: WindowState.Window2State => "lightblue"
					case _ => "inherit"
			),
		)

object Window1State:
	def apply(input: Signal[Window1State]): HtmlElement =
		???

object Window2State:
	def apply(input: Signal[Window2State]): HtmlElement =
		???
```

We see here a general pattern for constructing an application in a React-like way. Components are just functions from a signal to an element (defined in the above example as objects with `apply` methods). Each component function can route its input signal, narrowing the state down to the parts relevent to each of its child. DOM event listeners can be mapped to domain events and wired into event handlers.

For an example of a more elaborate application see the [To-Do list app](laminar-react/js/src/main/scala/todo/AppComponent.scala) in this repository.

## Conclusion

I hope this article has made clear how Laminar can be used to construct a front-end application in the style of React: as a (relatively) straight-forward declarative function from state to view, propagating the state through child components, which can themselves be understood as functions from properties to views. In the case of Laminar, our application is a function from a `Signal` of the state to a view, which  propagates the state through components that are functions from `Signal`s of properties to views. These signals have to be routed using a special syntax for conditional rendering, but once this is done, the rest works more or less the same way.

While airstream's reactive data does have some cognitive overhead and conditional routing does require a bit more ceremony than we're used to, the result is well worth the trouble. Airstream is an incredibly powerful and reliable reactive framework. Once you become familiar with the different data types and their rich menus of combinators, managing your application's dynamics will be substantially easier and more enjoyable than React or any JS/TS framework for web development. Moreover, Scala's syntax is much friendlier to React's functional approach to web development than JS/TS, as its algebraic data types (enums/sealed trait hierarchies) and pattern matching make it possible to model your application state with greater precision while simplifying the conditional logic needed to use it.


