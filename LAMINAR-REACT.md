
# Scala.js for React developers

> Should I read this?

Do you have front-end experience using React and some familiarity with Scala? Are you interested in trying frontend development Scala.js? If so, this could be helpful for you.

> Do I need to read the whole thing?

If you are already familiar with Laminar skip to ["Translating React components"](#translating-react-components) or ["Disciplined rendering"](#disciplined-rendering) to learn how to translate React patterns into Laminar. Also feel free to just [skip to the code](laminar-react/js/src/main/scala/). See [README.md](README.md) for instructions on running it.

## Background

It's no secret that Javascript is unsuitable for the scale it has reached. TypeScript too, while a huge improvement on JS, does not take advantage of many features that are gaining traction in modern programming languages. It was therefore a great relief to me as a Scala developer when a few years ago I had the option of using [Scala.js](https://www.scala-js.org/) as a browser language for a front-end task that had fallen in my lap. I was not disappointed in the results. Ever since that first opportunity, I have consistently found Scala.js to be a far more productive for web development than its alternatives. As time has passed, moreover, the trade-offs this choice entails (which were substantial at first) have steadily diminished. Bundle sizes have consistently shrunk (and bundle-splitting is now supported), compilation times have shortened, and tooling has improved dramatically. By this point, Scala.js has the footprint of a framework like React and is absolutely useable as a production solution.

When I first started using Scala.js, I had already used [React](https://react.dev/) on some other projects, so I decided to use scalajs-react, a Scala wrapper around the React framework. This would allow me to leverage the vast ecosystem of React components while using an architecture I was already familiar with. While this worked fairly well, I was nagged by the following thoughts:
1. Scala is great for designing abstractions -- especially for things like what React is doing, namely, modeling state updates via immutable data. It seems crazy to be relying on a JS solution for what Scala really shines at.
2. Since Scala.js already represents a framework's worth of overhead, it seems crazy to rely on another framework underneath it. (Using React in Scala.js requires adding a substantial Scala layer on top of it.) For Scala.js ever to become a serious contender, it should have its own abstraction for DOM rendering.
3. Scala's excellence in abstraction makes relying on community solutions less important. It was so easy to design my own windowed list in Scala.js, for instance, I skipped react-virtualized. Writing façades for the JS components often required about the same amount of work as rolling my own. If I could only find good Scala-only libraries that could *style* my application (like material UI), there would be no need for the ecosystem.

For these reasons I was really excited when [Laminar](https://laminar.dev/) was released -- the first Scala-only library for designing browser applications with real traction. Not only did Laminar mean React was not required, but the introduction of [web components](https://developer.mozilla.org/en-US/docs/Web/API/Web_components) has made it possible to use JS libraries without needing a JS framework. Some popular web-component libraries already have Laminar wrappers (I recommend [Web Awesome](https://github.com/nguyenyou/webawesome-laminar)), so I would not have to worry about the styling!

When a new opportunity to do some web-development in Scala came up more recently, I therefore decided to use Laminar and ditch React entirely. This experience eventually led to great results, but I admit it took me a while to work out how to get laminar to do what I wanted. It is a fairly unopinionated library, and there a lot of ways to go wrong using it. It also took me a while to come to grips with the implications of its significant differences with React. After some effort, however, I worked out a way to get Laminar working for me better than scalajs-react ever did. Moreover, I did so without having to throw out the mental model I had inherited from React.

In this article I will share with you what I learned from this experience: I will provide a brief introduction to Laminar, and then provide a series of formulas for translating different kinds of React components to Laminar components. All of the examples discussed here are included in this repository and can be run locally in a browser. See the [README](README.md) for instructions on building and running the code. At the end the article, I will sketch a suggested application architecture. For a more complete example application, you can look at the [example To-Do List application](laminar-react/js/src/main/scala/todo/AppComponent.scala) including in this repository, which you can also run. The README instructions show how to run a hot-reloading dev server, so you can play around with the code and see how it changes the behavior in the browser.

## Laminar

Laminar is the most widely adopted web development library native to Scala.js. It allows the user to construct DOM elements via a declarative API that is both intuitive and flexible, and is shipped with its own reactive data framework (Airstream) for managing DOM events and updates. Here is what a simple Laminar application looks like:

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

As the above example illustrates, elements are constructed declaratively using functions named after their corresponding tags. These functions accept child elements, property settings, and event handlers as arguments. Property settings are defined using `:=` for static values and `<--` for reactive data. Setting properties using a reactive value like `Signal` or `EventStream` will ensure that the property is updated with each new value. DOM event handlers are constructed using `-->`, which pipes an event property (e.g., `onClick` or `onChange`) into a reactive `Observer`.

Scala's powerful type system and expressive functional syntax makes it easy to *safely* pipe data between reactive elements of various types. For instance, in the above example the reactive `Var` `nameState` exposes an `Observer` via `textState.writer` which accepts `String` inputs, whereas the event property `onInput` provides a stream of `Event`s. The method `mapToValue` transforms this to a stream `String`s by extracting the input value from each event (it is equivalent to `onInput.map(_.currentTarget.value))`). Now that it matches the observer's type, it can be bound to it using `-->`.

If this all makes sense to you, great. If not, I recommend [taking a look at the docs](https://laminar.dev/documentation#introduction) (which are quite good) and trying out some code. As mentioned above, you can run the code in this repository with hot-reloading enabled, which provides a good way to play around with laminar code and see what it looks like. The entrypoint is [App.scala](laminar-react/js/src/main/scala/App.scala); to render something of your own, just comment out the expression under `val element =` and replace it with whatever you want to try out.

## General principles

The remainder of this article will try to demonstrate, through a series of examples, how to translate React patterns to Laminar. The approach sketched here is a fairly opinionated one. I strongly recommend it for new users, but there are other perfectly valid ways of structuring Laminar applications. There are some parts of my approach, however, that I would like to call out in advance as non-optional. It will save you a lot of trouble to get used to these three principles early on:

### 1. Data should always much always be reactive

In React, we are used to defining our components as functions from properties to DOM elements. In Laminar, components are functions from `Signal`s of properties to DOM elements. This makes things challenging at first, and it can often be tempting to try to skip the reactive types and render elements directly from their static properties. It is possible to do so in many cases, because you can map reactive state to rendered elements and bind them as children to parent nodes as follows:

```scala 3
div(
    child <-- someSignal.map(value => renderFromValue(value))
)
```

This should be avoided! The reason we are able to render directly from properties in React is our rendering functions don't actually render anything in the DOM. Instead, they render a *virtual* DOM. By comparing the virtual DOM before and after each change of state, React determines what has changed and propagates only those changes to the actual DOM. This way it renders only what needs to be rendered. In Laminar, there is no virtual DOM, no diffing, and no such automation for deciding what to render. The above example, for instance, will call `renderFromValue` any time `someSignal` is updated (even if the value doesn't actually change!) and will reconstruct from scratch whatever element it produces.

This distinction between React and Laminar has a number of important consequences, but the first that you need to learn is to get comfortable passing around reactive data types for just about everything. If you find yourself handling properties or state without it wrapped in a `Signal` wrapper, you're likely doing something wrong.

### 2. Think about rendering

To generalize the above point: get used to thinking about rendering. React encourages you not to think about it. Indeed, this is one of React's main value propositions, that it abstracts from the problem of rendering. The good news is that there are really only a couple of basic patterns dealing with rendering issues that can be learned quickly. We will go over these patterns in some of the examples below. Once you're comfortable with them, you will actually find Laminar to be safer than React since it is much clearer where rendering issues will arise and because you have much more control over rendering.

### 2. Events over callbacks

In React, we handle events via callbacks, calling functions like `setState` and `dispatch` that may also log and execute any number of other "side-effects." It is possible to use this approach with Laminar, since event props like `onClick` and `onChange` can be bound to callbacks, e.g.:

```scala 3
val state = Var[Boolean](true)

button(
    "Click me!",
    onClick --> { evt =>
        println("Hello world!")
        state.update(!_)
    }
)
```

The above is perfectly valid Laminar code, but you should avoid it. Instead, try the following:

```scala 3
val state = Var[Boolean](true)
val stateFlipper = state.updater[Any]((current, _) => !current)

button(
    "Click me!",
    onClick.tapEach(_ => println("Hello world")) --> stateFlipper,
)
```

What was wrong with the callback? After all, the second version looks like a more complicated version of the first. To be sure it is! The problem never appears in the simple cases. The problem is that once you start to construct your elements with a callback style, you end up missing out on Laminar's power when -- as your application scales -- it becomes really useful.

So what's the advantage of separating the `println("Hello World!")` statement from the state update? For one thing, it allows you to reuse `stateFlipper` without including that logging. `tapEach`, for its part, is explicit about the fact that it is *only* doing a side-effect. `onClick.map` could transform the `MouseEvent` value, but `.tapEach` will necessarily leave the event unchanged. It thus provides some clarity and safety.

More importantlly, in my view, by designing your application logic around event streams ran than side-effecting processes, you will be able to do more. The `tapEach` and `stateFlipper` "sections" of the little mouse click handling pipeline that we're constructing can be more easily pulled apart, reused, and combined in various interesting ways. Laminar's reactive combinators are powerful and flexible, and can do a ton of cool things. If you stick too closely to React approach, you're going to miss out on a lot.

## Translating React components

Let's start with a simple functional React component. In Javascript or TypeScript this is going to look
something like the following:

```typescript
type ComponentNameProps = {
    value1: string,
    value2: number,
    ...
}

function ComponentName(props: ComponentNameProps) {
    return <div>
        ...
    </div>
}
```

In Laminar, we'll do essentially the same thing: each component will be a function of some inputs that will return an `HtmlElement`, a Laminar representation of a DOM element. Instead of defining this function as `def ComponentName(...): HtmlElement`, however, let's construct it as follows:

```scala 3
import com.raquo.laminar.api.L.{*, given}

object ComponentName:
    def apply(...): HtmlElement =
        div(
          ...
        )
```

This is simply a more explicit (and verbose) way of defining `ComponentName` as a function object by implementing an `apply` method. The advantage of constructing it this way is that you can use the object `ComponentName` as a namespace for any type definitions or other utilities needed for it. For instance, you can define `Props` and/or `State` data types to model your inputs and state.

### Simple presentation components

Before we start defining typed inputs for our components analogous to those typically seen in React components, however, we should consider whether we really need to. For simple presentation components, I have found it makes more sense simply to reuse the existing Laminar API.

Consider, for instance, the `Stack` component from the [material-ui](https://mui.com/material-ui/) React library. This abstraction of the flex-box API has a lot of custom properties you can configure. Instead of adding a bunch of properties to our component, however, we can just do the following to add custom flex-styled components:

[laminar-react/js/src/main/scala/common/style/package.scala](laminar-react/js/src/main/scala/common/style/package.scala)
```scala 3
import com.raquo.laminar.api.L.{*, given}

object Flex:
    def column(modifiers: Modifier[HtmlElement]*): HtmlElement =
        div(
            display.flex,
            flexDirection.column,
            gap := "20px",
            alignItems := "stretch",
            cluster,
            modifiers,
        )

    def row(modifiers: Modifier[HtmlElement]*): HtmlElement =
        div(
            display.flex,
            flexDirection.row,
            gap := "20px",
            alignItems := "center",
            cluster,
            modifiers,
        )

    val cluster: Modifier[HtmlElement] = Seq(
        justifyContent := "flex-start"
    )

    val split: Modifier[HtmlElement] = Seq(
        justifyContent := "space-between"
    )
```

`row` and `column` take the usual modifiers and simply pass them along to the `div` element. They add a few custom modifiers first to customize the `div`. By putting `modifiers` last in the list, the user is able to override any of them. These two methods provide simple utilities to generate a `Stack`-like component that can be easily customized.

`cluster` and `split` are modifiers that can be added to `Flex.column` or `Flex.row` (or any other flex element) to customize them further. You can easily define these custom options as sequences of modifiers, which Laminar will implicitly convert to a single `Modifier`.

Note that instead of using the `apply` method on an `object` to define `row` and `column` components, we simply make them regular methods. I have found this a useful convention: lower-case functions for "tag"-like components, defined within a namespace (e.g. the `Flex` object above) that includes any options (`Modifier`s) that can be applied to them. These can either be imported from the namespace, or the namespace can be imported to avoid name collisions and make it easier to find needed methods using dot-completion in your IDE.

I have found handling styling through this type of component abstraction far more productive than trying to abstract all the specific properties I think the component should support. While it is not as precisely typed as we Scala developers may like, it's much easier to extend (which you have to do a lot with styles). By simply adding variants of the modifiers that can be mixed in (like `cluster` and `split`), you can add options to your styles without really having to change anything else.

You can find more examples of custom presentation components in the [styles package](laminar-react/js/src/main/scala/common/styles/package.scala). These components will be used consistently in the all of the examples in the rest of the article. Some of them depend on css styles that they're bundled with, which you can find [in the jsbundler directory](laminar-react/js/jsbundler/styles.css).

### Data in and data out

When you start designing components connected with the domain of your application, or presentation components with narrower uses, it makes sense to start defining your components' parameters with precise types rather than using `Modifier` parameters.

#### 1. Stateless component

The simplest case of an explicitly typed Laminar component is a stateless and eventless component. Consider a component whose purpose is only to display a header and text:

```scala 3
import com.raquo.laminar.api.L.{*, given}

object StatelessInput:
    final case class Props(
        header: String,
        body: String,
    )

    def apply(in: Signal[Props]): HtmlElement =
        Flex.column(
            h1(
              text <-- input.map(_.header),
            ),
            p(
              text <-- input.map(_.body),
            )
        )
```

In the above example, `Props` describes all the data that the `StatelessInput` component will render. The component is then defined as a function of a `Signal[Props]`, which is then piped into the appropriate DOM properties.

The alternative is to split the properties into separate signals that can each be an argument. This would look like the following:

```scala 3
...
    def apply(header: Signal[String], body: Signal[String]): HtmlElement =
        ...
```

In a lot of cases this would work just as well (in some cases even better), but I have found that in general it's better to include all required data in a single type. There are several reasons for this. First, while it is quite simple to *select* data from a signal of case class using `.map(_.propertyName)`, it is not as simple to combine signals, which you will often have to do if you segregate your data streams. To do this you have to use `signal1.combineWith(signal2)` which generates a signal of a tuple of both. Tuples more awkward to deal with than case case classes and are less safe. 

Most importantly, however, sticking with a single signal as your input will make it easier to adapt to more complicated data models. `Props` can be an `enum` when your component's state has different modes with different properties (which is often the case). It is easier to model your components more reasonably when you start out with consolidated inputs.

One final note: I have used `in` as the name of the props signal parameter. I have found this a useful convention (`input` is the name of a tag, so avoid that). While it is not super important what name you choose, I encourage you to be consistent. Applications and individual components quickly get complicated, and it makes everyone's lives easier when the can depend on regular conventions across the codebase. One of the great things about React is you can open up pretty much any React codebase and quickly see how it all works. By using the same parameter name (`in`) and the same type name (`Props`), it is very easy for a newcomer to get an idea of what's going on in each component.

#### 2. Controlled stateless component

We have seen the simple case where we have a component with data coming into it in the form of `Props`. That component was not interactive, however, which is to say it didn't expose any events for its parent to handle. What would an interactive component look like? The typical pattern for a data-in/data-out component is one with "controlled" properties. It does not have its own state, but it allows its parent to set a value and then also handle events that indicate a change in that value. The example below provides a simple example of this, with a controlled text input:

[laminar-react/js/src/main/scala/article/Example2StatelessInputOutput.scala](laminar-react/js/src/main/scala/article/Example2StatelessInputOutput.scala)
```scala 3
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
                onClick.compose(_.withCurrentValueOf(in).map(t => Event.SubmitValue(t._2.value))) --> out,
            ),
        )
```

Whereas in React we would include separate callbacks as properties to handle the different events that may may be triggered in our component, in Laminar we can require parents to provide only a single handler `out`, which handles *all* the events. This is possible if we model our events as a single `enum` type with a different case for each distinct event type. The "handler" `out` is a `Sink[Event]`, which essentially means any Airstream type that can *consume* an `Event`. As [discussed above](#2-events-over-callbacks), we are going to think of our components dynamics in terms the flow of data (`EventStream`s and `Sink`s) rather than in terms of handlers (i.e., callbacks).

Declaring our events under a single `enum` type (or a `sealed` class hierarchy if you're using Scala 2) and exposing them in a single handler has additional benefits. It encourages parent components to provide a coherent logic to how it uses its components. Rather than implement its handlers piecemeal, it will generally choose to match on the events in one place and determine what actions to take, our how to translate them into its own proper event type (which is generally the best structure your event logic). This makes refactoring easier: if you need to change how you model your events, it is very easy to trace your way through the upstream logic that needs to be updated to accomodate those changes. It is easier to make those updates as well when every component has its entire event-handling logic together in one place. You can quickly see how changing the way one event is handled may require changes to how another event is handled, and what other changes may be needed.

One somewhat complicated part of in the example above is rendering the submit button. This button's `onClick` event property needs to be mapped to a `Event.SubmitValue` which requires including the current input `value`. This requires accessing the state, which is only accessible via the signal. To access the state signal from a DOM event, we have to combine it with the event property's underlying event stream using `compose`: `onClick.compose(_.withCurrentValueOf(in).map(t => Event.SubmitValue(t._2.value))) `. `compose` accepts a function transforming the event stream, which can be combined with the signal using the `withCurrentValueOf` method. Then `map` is used to extract the item number (the third member of the resulting tuple) and wrap it in `RemoveItem`.

### Disciplined rendering

Now that we've covered the basics of component design and can construct components that can support arbitrary dynamics (even if they themselves don't maintain the state needed to implement those dynamics -- we'll get to that later), we should cover what may be the most important subject in this article: how to ensure that components render elements only when they need to be.

As [discussed above](#2-think-about-rendering), Laminar does not provide a virtual DOM to think about rendering for you, so you need to be careful how you design your component's rendering logic. Fortunately there just a small number of tricks that, if used consistently, will prevent over-rendering from ever becoming a problem.

#### 3. Rendering arrays

The first and most obvious case of rendering issues for someone coming from React has to do with rendering DOM arrays. In React, this is one place where you always need to take some action to ensure rendering works properly. The reason is that when you have a single element in your JSX, React can always compare the element before and after a change of state; but when you construct an array of elements on each state change, React doesn't necessarily know which elements to compare between the first array and the second. To help it do so, you have to provide a "key" on each element in the array so that React can look up comparable elements.

While Laminar does not have a virtual DOM to compare against, it does provide an equivalent strategy for dealing with the same problem. On any `Signal` of a *collection* of values (any standard Scala collection will work), it exposes a `split` method, which allows you to (1) map each member of the collection to a key, and (2) render each element using the key and a signal *of the member value*. The function you provide to render the element will only be called once for each member in the collection whose key remains consistent across updates, and the changing values of that member will be exposed in the signal. Only when then original signal fails to produce a given key will the corresponding element be unmounted; the next time it appears, the rendering function will be called again.

This is what the split method looks like in action:

[laminar-react/js/src/main/scala/article/Example3RenderArray.scala](laminar-react/js/src/main/scala/article/Example3RenderArray.scala)
```scala 3
object RenderArray:
    final case class Entry(label: String, text: String)

    final case class Props(
        header: String,
        body: List[Entry],
    )

    def apply(in: Signal[Props]): HtmlElement =
        val bodyEntries: Signal[List[HtmlElement]] = in
            .map(_.body)
            .split(_.label): (label: String, _: Entry, entrySignal: Signal[Entry]) =>
                li(
                    Flex.row(
                        input(`type` := "checkbox"),
                        div(label),
                        div(
                            text <-- entrySignal.map(_.text),
                        )
                    )
                )

        Flex.column(
            h1(
                text <-- in.map(_.header),
            ),
            ul(
                children <-- bodyEntries,
            )
        )
```

In the above example, the `body` property contains a `List` of entries (each containing a `label` and some `text`). The body is rendered in `bodyEntries`; to generate the keys, each `Entry` in `body` is mapped to its `label`. This implies that `label`s are expected to be *unique*, so that the same label will only ever appear once in the collection and can be used to track the same element across state changes. If `label` were not expected to be unique, we would probably want to include the element index as part of the key.

After the key is generated using `.split(_.label)`, a function is provided with three parameters: `label`, which is the key; `_`, an unused parameter containing the full initial value of the `Entry`; and `entrySignal`, a signal of `Entry` values *for the element corresponding to this particular `label`*. The unused `Entry` parameter comes from the initial value of the signal when the key (or `label`) first appears. It allows you to render properties statically in cases where you know they won't change. Since the only static property we need is the `label`, and this is provided as the key parameter, we don't use it. Everything else is rendered from the `entrySignal`.

`split` may seem confusing at first; it helps to think of it simply as a specialized `map`. It maps each member of a collection to an element, but does so in a way that handles the rendering problem, routing the state changes for each of the unchanged members of the collection (as determined by your key function) to your rendering function in the form of a signal, [which of course is what we always want](#1-data-should-almost-always-be-reactive).

Note that the result type of `split` in the above example is `Signal[List[HtmlElement]]`. It generates a *signal* of elements in the *same collection type* as the original signal. A signal of a collection of elements can be rendered as children of any other element using the `children` receiver. So in at the bottom of the example `bodyEntries` is rendered using the expression `children <-- bodyEntries`.

It is possible also to generate signals of collections of Laminar elements without `split`. We could render directly from the props signal, for instance, which would look like the following:

[laminar-react/js/src/main/scala/article/Example4RenderArrayBad.scala](laminar-react/js/src/main/scala/article/Example4RenderArrayBad.scala)
```scala 3
...
object RenderArrayBad:
    ...
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
    ...
```

Note that in the above example, both `label` and `text` are rendered statically as regular strings -- not bound as reactive inputs with `<--`, since of course they are not reactive. This fact alone reveals immediately that the `li(Flex.row(...))` element is going to be re-rendered on each state change.

To see how this affects the component behavior, run "Example 3" and then "Example 4" in the demo application from this repository. Both examples use the same inputs: every ten seconds a new random set of entries are generated, every 1.5 seconds those entries are shuffled, and every 4 seconds a new header is provided. This allows us to see how different kinds of changes affect the rendering in different ways. Each entry includes an uncontrolled checkbox, which allows us to test rendering. Since the state of that checkbox is maintained by the element, it will be cleared each time the element is rerendered.

We notice in the "good" example (Example 3), checkboxes that we click stay checked as the list of entries is shuffled. The only time they are cleared is either when we click them again or when the entire list to changed after ten seconds. In the latter case, Laminar throws out the old entries and re-renders the new ones because the new entries have new `label`s.

In the "bad" example, on the other hand, every check mark is cleared every time the list is shuffled. This is because each element is rerendered from scratch on each state update, most notably when the list is shuffled (this is the most frequent state change). When you render this way, any time *anything* changes in the input signal, you are going to lose any state in the rendered elements, whether that state is maintained in the DOM (as in this case), or is maintained by the Laminar component. Even if you don't care about that, however, (say *all* of your state is propagated through the original signal), it can affect the performance of your application to rerender *every* element of your list with every update. *Make sure you use `split` to render collections*.

#### 4. Simple conditional rendering

While most people coming from React should be aware of the need to render arrays cautiously, they will likely be less prepared to address the next challenge, which is rendering *individual* elements *conditionally*. The basic difficulty is this: if some of your input data must be rendered very differently depending on its current value, you will have to sometimes rerender as the properties change. The problem is that you don't want to rerender the whole thing every time *any* property changes, but only those properties that determine which of the rendering modes should be used.

In effect, Laminar has the same problem for conditional rendering that Laminar and React both have for rendering arrays: it needs some way to determine when a single element, which *sometimes* needs to be rerendered, should be considered the *same* across changes of state.

There are three basic strategies for dealing with this problem. The first two are nearly equivalent and can be used for simple cases like the following:

[laminar-react/js/src/main/scala/article/Example5ConditionalRenderSimple.scala](laminar-react/js/src/main/scala/article/Example5ConditionalRenderSimple.scala)
```scala 3
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
```

The above component renders the same data differently depending on the input value `choice`, an `enum` with two possible states: `Choice.One`, and `Choice.Two`. A select element is provided to choose between the two choices. Two different parts of the component are rendered differently depending on the choice: the `headerElement` and the `choiceElement` (i.e., the body), allowing us to showcase two different ways of approaching the problem. Each of these elements are constructed as signals of `HtmlElement`s and are rendered in the final element using the `child` receiver (`child <-- headerElement` and `child <-- choiceElement`) just like the `children` receiver we saw above.

The simplest way to correctly render conditionally can be seen in the definition of `headerElement`, which simply maps the input to the `choice` property, calls `distinct` on the result, and then maps again. If `distinct` were left out, this would not work properly, as every state change would lead to subsequent `map` function being called and generating a new version of the same element. When we call `distinct`, however, we generate a new version of the same signal that filters out any updates that do not actually change the value of the signal. The only time this new signal will propate through the last call to `map` is when a new `Choice` is provided.

A similar but slightly more powerful version of the same thing is shown in `choiceElement`. Here the signal is rendered using `splitOne`, which is similar in form to `split`: the signal is mapped to a key (`choice`, in this case) and then the key, initial value, and signal is rendered. Unlike `split`, however, the signal that's passed is not meaningfully different from the original, so it's not especially useful. When using `distinct` you have access to both the key and the relevant signal, so the only thing that `splitOne` provides that `distinct` doesn't is access to the full initial static value of the signal for each rerendering. I rarely use that, so it's usually not much of an advantage.

Nevertheless, I still prefer using `splitOne` simply because it's more explicit about what's going on. You can delete the `.distinct` without breaking anything else in your code, which could be easy for someone to do who is not familiar with the code base, and not notice a problem until runtime. Due to its similarity to `split`, `splitOne` makes it a little more clear that it's addressing the rendering problem, and it's harder to overlook or accidentally remove.

As with rendering arrays, it is very easy to *not* render conditional elements correctly as we can see in the following *bad* example:

[laminar-react/js/src/main/scala/article/Example6ConditionalRenderSimpleBad.scala](laminar-react/js/src/main/scala/article/Example6ConditionalRenderSimpleBad.scala)
```scala 3
...
object ConditionalRenderSimpleBad:
    ...
        val choiceElement = in.map:
            case Props(Choice.One, header, body) =>
                StatelessInput(Val(StatelessInput.Props(header, body)))
            case Props(Choice.Two, header, body) =>
                Flex.row(
                    alignItems.start,
                    input(`type` := "checkbox"),
                    div(header),
                    div(
                        overflowX.auto,
                        body,
                    ),
                )
    ...
```

Once again, we end up passing static (non-reactive) values to the components we render, which is a sure sign that we're doing something wrong. In fact, just to make this work, we have to wrap the props we pass to `StatelessInput` in `Val`, which is a reactive wrapper around unchanging data. This should only be used when you want to render something that requires a reactive element from data that really isn't ever changing. The fact that we're forced to use it here should immediately tell something isn't right.

Running Example 4 and 5, you can see the effects for yourself. In Example 4 (the correct version), as new data is piped into the element every second or so, the state of the checkbox next to the header does not change unless you select a new choice from the drop-down menu. When you select "Choice 2", you will see another checkbox next to the text rendered at the bottom. This one will also remain consistent as long as the choice does not change. If you try the same thing in Example 5, however, you will find that every single update to the text clears both checkboxes, even when we haven't changed the choice.

#### 5. Complex conditional rendering

While `splitOne` (or `distinct`) is sufficient to address a lot of typical scenarios, you will find that as your application state gets more complex and precise (as it should!), they sometimes don't work well. Consider what happens when your `Props` are not a straightforward case class, but an `enum`:

[laminar-react/js/src/main/scala/article/Example7ConditionalRenderComplex.scala](laminar-react/js/src/main/scala/article/Example7ConditionalRenderComplex.scala)
```scala 3
object ConditionalRenderComplex:
    enum Props:
        case Choice1(header: String, body: String)
        case Choice2(header: String, body: List[(String, String)])

    enum Event:
        case Choose1, Choose2

    def apply(in: Signal[Props], out: Sink[Event]): HtmlElement = {
        val choiceElement = in
            .splitOne(???): (_, _, signal) =>
                ???

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
```

The above example is very similar to the previous example, except instead of the props having the same shape for each choice, each choice corresponds to an entirely different set of properties. This is actually one of the great things about using Scala instead of JS/TS, as this is often a *much* clearer and safer model of many scenarios than a flat set of properties where certain properties discriminate between "modes" (e.g., `choice` from the previous example) and other properties only have meaning within certain modes. The problem, however, is that modeling the data more precisely makes it considerably tricker to "split" the data signal. Here is what we would have to do:

```scala 3
val choiceElement = in
    .splitOne({
        case _: Props.Choice1 => 1
        case _: Props.Choice2 => 2
    }):
        case (_, initial: Props.Choice1, signal) =>
            val typedSignal = signal.changes.collect {
                case ch1: Props.Choice1 => ch1
            }.toSignal(initial)
            StatelessInput(typedSignal.map(p => StatelessInput.Props(p.header, p.body)))
        case (_, initial: Props.Choice2, signal) =>
            val typedSignal = signal.changes.collect {
                case ch2: Props.Choice2 => ch2
            }.toSignal(initial)
            val renderArrayIn = typedSignal
                .map(p => RenderArray.Props(p.header, p.body.map(RenderArray.Entry.apply.tupled)))
            RenderArray(renderArrayIn)
```

This approach -- *which I do not recommend* -- requires matching on the input signal *three different times*! First, we have to match on it to generate some arbitrary key corresponding to each subtype, or mode (in this case I use integers 1 and 2). Next, we have to match on the initial value of the input signal in our rendering function to determine which mode we're in. Finally, we have to narrow our signal down to the relevant case using `collect` in order to render anything from it. This is much too verbose.

Fortunately, Laminar provides us with a macro-based solution `splitMatchOne` that allows us to avoid all this redundant matching:

[laminar-react/js/src/main/scala/article/Example7ConditionalRenderComplex.scala](laminar-react/js/src/main/scala/article/Example7ConditionalRenderComplex.scala)
```scala 3
object ConditionalRenderComplex:
    ...
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
    ...
```

Here we call `splitMatchOne` on the input signal `in`, returning a special utility that allows us to handle each case separately. First we handle `Choice1` using `handleCase`, which allows us to match a case and transform it using a partial function and then render the transformed signal. This is useful for `Props.Choice1` because it lets us easily construct the `StatelessInput.Props` that we intend to render.

In the second case, we use `handleType`, which allows to specify the subtype we want to match without having to provide a partial function at all. We can see in the example why it's actually better in this case to use `handleCase`, as it would simplify the preparation of the `RenderArray.Props` (I included `handleType` here just for demonstration). `handleType` is useful for simpler cases where you don't need to do any complicated mapping of the signal. I typically use `handleType[Any]((_, _) => emptyNode)` at the bottom of a `splitMatchOne` expression, for instance, to render an empty default case.

Once again, it is easy to see how this can be done incorrectly:

[laminar-react/js/src/main/scala/article/Example8ConditionalRenderComplexBad.scala](laminar-react/js/src/main/scala/article/Example8ConditionalRenderComplexBad.scala)
```scala 3
...
object ConditionalRenderComplexBad:
    ...
        val choiceElement = in.map:
            case Props.Choice1(header, body) =>
                StatelessInput(Val(StatelessInput.Props(header, body)))

            case Props.Choice2(header, entries) =>
                RenderArray(Val(RenderArray.Props(header, entries.map(RenderArray.Entry.apply.tupled))))
    ...
```

Note how much much simpler it is, in this case, to simply match on the original state without the indirection of `splitOne` or `splitMatchOne`. As you first work on Laminar components, you may have a strong inclination to construct elements this way. Make sure you get used to reaching for `splitOne` and `splitMatchOne` any time you need to render a different element for different state.

The good news is that once you get used to always using `split`, `splitOne`, and `splitMatchOne`, you actually don't have to give much thought at all to rendering. Moreover, any time you run into rendering problems -- say components are losing state unexpectedly -- it's not hard to trace it to a `map` that needs to turn into a `splitX`. Everything else can then be thought about in more or less the same way you think about a React application: as a function from state to DOM. The only difference is that you have to used a specialized mechanism for matching on that state.

As a final note, I'll mention the `splitMatchSeq` method that provides a similar API as `splitMatchOne` but for rendering arrays. It's very similar to `split` and `splitMatchOne` (in fact, it is equivalent to using these together), so I decided not to provide a separate example for it, but you should keep it in mind for when you need to render arrays of ADTs (`enum`s or `sealed` class hierarchies). If you are comfortable with the other `splitX` methods, it will be clear how to use it.

To conclude, let's break down the different kinds of scenarios where we need to be careful about rendering, and which specialized function to use in place of `map`:
1. `splitOne`: rendering a single element in different modes (depending on a signal's value) when the different modes do not require filtering the input signal by subtype
2. `splitMatchOne`: rendering a single element in different modes when the different modes *do* require filtering the input signal by subtype
3. `split`: rendering a collection of elements from a collection of data, where there is either a single rendering mode or the different modes don't require filtering the input signal by subtype
4. `splitMatchSeq`: rendering a collection of elements from a collection of data, where there are multiple modes of rendering that require filtering the input signal by subtype

As we can see, the main two considerations are (1) whether we're rendering collections (`split` and `splitMatchSeq`) or single elements (`splitOne` and `splitMatchOne`) and (2) whether the different ways we plan to render a given element all use the original signal corresponding to that element (`split` and `splitOne`) or require the signal narrowed down to distinct subtypes (`splitMatchOne` and `splitMatchSeq`).

### State management

Now that we have covered data input/output and disciplined rendering, all that remains is using state. Once we are able to manage state in a component, we will be able to construct a complete application.

#### 5. Simple state

The basic reactive data type for state is `Var`. `Var` is a container for a value that can be changed using `set` or `update`. It's current value can be accessed via the `now()` method. However, in keeping with our preference for [events over callbacks](#2-events-over-callbacks), instead we will use the `signal` method for reading state as a `Signal`, and the `writer` and `updater`, for writing state via `Observer`s (`Observer` is a kind of `Sink` that data can be piped into via `-->`).

Here is the simplest form of a stateful component:

[laminar-react/js/src/main/scala/article/Example9StatefulSimple.scala](laminar-react/js/src/main/scala/article/Example9StatefulSimple.scala)
```scala 3
object StatefulSimple:
    final case class Props(
        header: String,
        body: String,
    )

    val collapsedState: Var[Boolean] = Var(false)

    def apply(in: Signal[Props]): HtmlElement =
        Flex.column(
            h1(
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
```

This example component provides a collapsible view of some text. The header is always displayed, while the body is ownly displayed when the value of the `Var` `collapsibleState` is false. This state is controlled by a checkbox, whose `checked` state is pulled from `collapsibleState`'s `signal`, and whose `onChange` events are piped into `collapsibleState`'s `writer`. `writer` is a simple `Observer` that sets the `Var` to the observed value. The body is then rendered conditionally based on the current state of `collapsedState`: when it is `true` and `emptyNode` is rendered, and when `false`, a `div` containing the input body is rendered.

The result, which you can see by running "Example 9," is equivalent to a standard controlled React component. When you click the the checkbox, it's checkmark appears and the body text disappears. When you click again, the checkmark disappears and the body text reappears.

#### 6. Complex state

The above example is enough, in principle, to show you how to construct any application: just throw the initial state in a `Var` and use it's `signal` and `writer` as inputs and outputs for your components. There are a number of ways that a `Var` can be used, however, and some of these ways can not work very well as your application scales in complexity. In particular, it is easy to lose sight of the [events over callbacks](#2-events-over-callbacks) principle as it's not necessarily obvious how to control state via events as your state model becomes more complex. The prior example provides an uncommonly simple case where the state `Boolean` corresponds directly to the `onChange.mapToChecked` event processor. What about when your state is more elaborate?

To see how more complicated cases can be handled properly, let's work through the following example: a component that displays a list of text items, allowing the user to add new items, remove them, and shuffle their order. This should be complicated enough to require a more elaborate approach while being simple enough to follow easily.

Let's begin by modeling our *state* and our *events*. This will look very similar to how we modeled our input `Props` and output `Event` [above](#2-controlled-stateless-component). In fact, we are doing essentially the same thing, since the state and events will indeed be inputs and outputs of the elements we're rendering. Now, however, will keep these types `private` since any parent components will not be able to use them and should not have to access them. We will call our event type `StateEvent` so that it won't conflict with public events if we decide to expose any. Finally, we will include a `reduce` function on `State`, that determines how the state is updated by each `StateEvent`.

[laminar-react/js/src/main/scala/article/Example10StatefulComplex.scala](laminar-react/js/src/main/scala/article/Example10StatefulComplex.scala)
```scala 3
object StatefulComplex:
    private enum StateEvent:
        case StartEditingItem
        case ChangeCurrentItem(newText: String)
        case StopEditingItem
        case AddCurrentItem
        case RemoveItem(index: Int)
        case Shuffle

    private case class State(currentItem: Option[String], existingItems: Vector[String]):
        def reduce(event: StateEvent): State = event match
            case StateEvent.StartEditingItem => copy(currentItem = currentItem.orElse(Some("")))
            case StateEvent.ChangeCurrentItem(newText) => copy(currentItem = Some(newText))
            case StateEvent.StopEditingItem => copy(currentItem = None)
            case StateEvent.AddCurrentItem => copy(
                currentItem = None,
                existingItems = currentItem match {
                    case None => existingItems
                    case Some(txt) => existingItems.appended(txt)
                },
            )
            case StateEvent.RemoveItem(index) => copy(existingItems = existingItems.patch(index, Nil, 1))
            case StateEvent.Shuffle => copy(existingItems = scala.util.Random.shuffle(existingItems))
```

These two type definitions, along with `State#reduce` determine the entire behavior of the component. Once this is defined, implementing the rest of the component is fairly straightforward:

[laminar-react/js/src/main/scala/article/Example10StatefulComplex.scala](laminar-react/js/src/main/scala/article/Example10StatefulComplex.scala)
```scala 3
object StatefulComplex:
    ...
    def apply(): HtmlElement =
        val state = Var(State(None, Vector.empty))
        val eventSink = state.updater[StateEvent]((state, event) => state.reduce(event))

        val addItemComponent = state.signal
            .splitMatchOne
            .handleCase({ case State(None, _) => () }) { (_, _) =>
                customButton(
                    Flex.row(Icon.add(Icon.small), "Add item", gap := "5px"),
                    onClick.mapTo(StateEvent.StartEditingItem) --> eventSink,
                )
            }
            .handleCase({ case State(Some(txt), _) => txt }) { (_, textSignal) =>
                Flex.row(
                    "Item text",
                    customInput(value <-- textSignal, onInput.mapToValue.map(StateEvent.ChangeCurrentItem(_)) --> eventSink),
                    Flex.row(
                        gap := "5px",
                        customButton(
                            "Add",
                            disabled <-- textSignal.map(_.strip.isEmpty),
                            onClick.mapTo(StateEvent.AddCurrentItem) --> eventSink,
                        ),
                        customButton(
                            "Cancel",
                            onClick.mapTo(StateEvent.StopEditingItem) --> eventSink,
                        )
                    )
                )
            }
            .toSignal

        def renderSingleListItem(itemSignal: Signal[(String, Int)]): HtmlElement = {
            val onClickMappedToRemoveItem =
                onClick(_.withCurrentValueOf(itemSignal).map(v => StateEvent.RemoveItem(v._3)))

            li(
                Flex.row(
                    input(`type` := "checkbox"),
                    text <-- itemSignal.map(_._1),
                    Icon.close(
                        makeIconButton,
                        onClickMappedToRemoveItem --> eventSink,
                    ),
                    gap := "5px",
                ),
            )
        }

        div(
            child <-- addItemComponent,
            ul(
                children <-- state.signal
                    .map(_.existingItems.zipWithIndex)
                    .split(_._1)(
                        (_, _, listItemSignal) => renderSingleListItem(listItemSignal)
                    )
            ),
            child <-- state.signal.map(_.existingItems.nonEmpty).distinct.map:
                case true => customButton("Shuffle items", onClick.mapTo(StateEvent.Shuffle) --> eventSink)
                case false => emptyNode
        )
```

State is stored in the `state` `Var`, which is constructed with an initial value. An event sink `eventSink` can be defined easily using the `updater` method on `Var`, which produces an `Observer` pretty much directly from `State#reduce`. The component's element tree is then rendered from `state.signal`, which exposes the current value of the `State` as a `Signal`. DOM event props are mapped to `StateEvent` values and piped into `eventSink`. No state logic needs to be included anywhere in the rendered expression: it's simply a matter of translating each DOM event to the appropriate `StateEvent`.

#### 8. Global state (Redux)

You may recognize that the above pattern from the popular React-redux combination, in which an event store is used to manage state and state is updated by dispatching actions. Generally, this state is stored and used at the top level of the application and accessed at any depth via React "context," which provides both the state and the required dispatch function. While this pattern is somewhat cumbersome to use in React for local state, in Scala it's sufficiently ergonomic that I highly recommend using it for local as well as global state.

Regardless, it can be useful to define a state and event model shared throughout the application in the same way Redux is used in React. To do this, let's begin by creating an abstraction for our state store. As we have seen, all we really need is an initial state and a reducer; we can then construct the inputs and outputs using `Var`. One useful thing we can add to this is an `EventBus`. By routing the event input through an event bus, we can also support subscriptions to all events that are dispatched. We can use for things like logging and persistence, or to handle certain events with I/O after updating state.

Here is what our abstraction would look like:

```scala 3
class StateContext[State, Event](
    initialState: State,
    reduce: (State, Event) => State
):
    private val stateVar = Var(initialState)
    private val stateUpdater = stateVar.updater(reduce)
    private val eventBus = EventBus[Event]()

    def input: Sink[Event] = eventBus.writer

    def events: EventStream[Event] = eventBus.events

    def state: Signal[State] = stateVar.signal

    // Needs to be bound to an element to work
    def bind: Binder[Element] = eventBus.events --> stateUpdater
```

Notice that we also include a `bind` method: this must be attached to an element so that the subscription of `stateUpdater` to `eventBus.events` is scoped to something with a lifecycle, as it's a resource that must eventually be cleaned up. We can do this by passing it to a tag, just as we do with any other event binding. (See the Laminar docs for [a larger discussion of subscriptions as resources](https://laminar.dev/documentation#ownership).)

Now we can define the global state and event model for interface in the same way as before:

```scala 3
enum AppEvent:
    case ...

final case class AppState(...):
    def reduce(event: AppEvent): AppState = ???

type AppContext = StateContext[AppState, AppEvent]
```

Since we are going to be accessing our state throughout our application, we also include here a type alias `AppContext` for the `StateContext` specific to our application.

Our application, then, must make an instance of `AppContext` available to any component within it that needs to use it. In React, this is accomplished by creating a [React context](https://react-redux.js.org/using-react-redux/accessing-store) at the top level, which any child component at any level can retrieve from the environment. In Scala, we have a more type-safe method of passing context, which is to provide it *implicitly* with `using` or "context" parameters. Every component that needs to use the `AppContext` just includes it in as a `using` parameter in its `apply` function: 

```scala 3
object ConnectedComponent:
    final case class Props(...)

    enum Event:
        case ...

    def apply(input: Signal[Props], output: Sink[Event])(using appStore: AppStore): HtmlElement =
        ???
```

Just as in React, these "connected" components can also have their own state and inputs and outputs separate from those of the global application context.

For any component to invoke a connected component, it must also be a connected component *or* it must construct an implicit context to pass to them. This should be done at the top level:

```scala 3
object TopLevelComponent:
    def apply(): HtmlElement =
        given appStore = StateStore[AppState, AppEvent](
            AppState(...),
            (state, event) => state.reduce(event),
        )

        propsSignal: Signal[ConnectedComponent.Props] = ???
        eventSink: Sink[ConnectedComponent.Event] = ???
    
        ConnectedComponent(propsSignal, eventSink)
```

And easy as that, we have a Redux-style application with global state and connected components.

#### 9. Anatomy of a laminar component in the React model

We have now covered how to translate all the basic React patterns into Laminar. Let's conclude with an overview of all parts of a Laminar component. This sketch can be used as a reference as you build an application. Note that not all parts included here should be included in every Laminar component, but every Laminar component should include some of these parts.

```scala 3
object Component:
    // Outputs and inputs

    enum Event:
        case ...

    final case class Props(...)

    // Local state and events

    private enum StateEvent:
        case ...

    private final case class State(...):
        def reduce(event: StateEvent): State = ???

    // Construct element from inputs, outputs, and global state
    def apply(in: Signal[Props], out: Sink[Event])(using appStore: AppStore): HtmlElement =
        // Initialize local state on each render
        val localState = StateStore(
            State(...), 
            (state, event) => state.reduce(event)
        )

        // Define element
        ???
```

While there is no necessity you use the naming conventions I have used here, I strongly encourage you to be consistent across your entire application. This will provide a reliable framework new contributors on your project can use to quickly work out what different components are doing.

## Conclusion

I hope this article has made clear how Laminar can be used to construct a front-end application in the style of React: as a (relatively) straight-forward declarative function from state to a DOM tree (a *virtual* DOM tree, to be precise), propagating the state through child components, each of which can also be understood as a function from properties to a DOM tree. In the case of Laminar, our application is a function from a *`Signal`* of the state to a DOM tree (or a Laminar representation of a DOM tree), which  propagates the state through components, each of which is also a function from a `Signal` of properties to a DOM tree. These signals have to be routed using a [special syntax for conditional rendering](#disciplined-rendering), but once this is done, the rest works more or less the same way.

While Airstream's reactive utilities do introduce some cognitive overhead and conditional rendering does demand a bit more ceremony than we're used to, the result is well worth the trouble. Airstream is an incredibly powerful and reliable reactive library. Once you become familiar with the different data types and their rich menus of combinators, managing your application's dynamics will be substantially easier and more enjoyable than React or any JS/TS framework for web development. Moreover, Scala's syntax is much friendlier to React's functional approach to web development than JS/TS, as its algebraic data types (enums/sealed class hierarchies) and pattern matching make it possible to model your application state with greater precision while also simplifying the conditional logic needed to use it.
