# laminar-react

Examples of Laminar components modeled after React. (See [LAMINAR-REACT.md](LAMINAR-REACT.md) for explanations.)

## Dependencies

To compile and run/deploy the code in this repository you will neeed at minimum the following installed in your development environment:
1. sbt
2. npm

## Run examples

To run the examples in a browser execute the following SBT command:

```shell
sbt "startDevServer; ~fastLinkJS/prepareBundleSources; stopDevServer"
```

This will compile all the code to Javascript, bundle it, and deploy in a browser. It will hot-reload any changes to the code, so feel free to play around with the examples and see how it changes the rendered application.

## Project structure

```
├── build.sbt
├── laminar-react
│   └── js
│       ├── jsbundler (bundle artifacts: html, css, JS entrypoint)
│       └── src
│           └── main
│               └── scala
│                   ├── App.scala (SPA entrypoint)
│                   ├── article (all examples from article)
│                   │   ├── Example10StatefulComplex.scala
│                   │   ├── Example1StatelessInput.scala
│                   │   ├── Example2StatelessInputOutput.scala
│                   │   ├── Example3RenderArray.scala
│                   │   ├── Example4RenderArrayBad.scala
│                   │   ├── Example5ConditionalRenderSimple.scala
│                   │   ├── Example6ConditionalRenderSimpleBad.scala
│                   │   ├── Example7ConditionalRenderComplex.scala
│                   │   ├── Example8ConditionalRenderComplexBad.scala
│                   │   ├── Example9StatefulSimple.scala
│                   │   └── Util.scala
│                   ├── common (styles and state management helper)
│                   │   ├── StateContext.scala
│                   │   └── styles
│                   │       └── package.scala
│                   └── todo (example To-Do list application)
│                       ├── AppComponent.scala
│                       ├── ChooseListComponent.scala
│                       ├── CompletedToDoComponent.scala
│                       ├── CreateListComponent.scala
│                       ├── CreateToDoComponent.scala
│                       ├── model
│                       │   ├── AppEvent.scala
│                       │   └── AppState.scala
│                       ├── package.scala
│                       ├── ToDoComponent.scala
│                       └── ToDoListComponent.scala
├── LAMINAR-REACT.md
└── README.md
```

## Article

See [LAMINAR-REACT.md](LAMINAR-REACT.md) for a discussion of how to use Laminar in a React-like style and explanations of the various examples.
