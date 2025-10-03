# laminar-react

Examples of Laminar components modeled after React

## Dependencies

To compile and run/deploy the code in this repository you will the following installed in your development environment:
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
├── LAMINAR-REACT.md (article explaining the code in this repository)
├── README.md (you're reading this)
├── build.sbt
├── laminar-react
│         ├── js (Scala.js source code)
│         │   ├── jsbundler (JS and other assets used for building deployable web page)
│         │   │     ├── index.html
│         │   │     ├── main.js
│         │   │     ├── package.json
│         │   │     └── public
│         │   │         └── logo.png
│         │   └── src
│         │       ├── main
│         │       │    └── scala
│         │       │        ├── App.scala (Entrypoint: allows choosing from components below)
│         │       │        ├── article (Examples from article)
│         │       │        │     ├── Example0Simple.scala
│         │       │        │     ├── Example1Naive.scala
│         │       │        │     ├── Example2SplitOne.scala
│         │       │        │     ├── Example3RouteSignal.scala
│         │       │        │     ├── Example4SplitArray.scala
│         │       │        │     ├── Example5CombineSignals.scala
│         │       │        │     └── Example6HandleWithSignal.scala
│         │       │        ├── todo (To-Do list application)
│         │       │        │     ├── AddListComponent.scala
│         │       │        │     ├── AddToDoComponent.scala
│         │       │        │     ├── AppComponent.scala (Top-level component)
│         │       │        │     ├── ChooseListComponent.scala
│         │       │        │     ├── DoneToDoComponent.scala
│         │       │        │     ├── ToDoComponent.scala
│         │       │        │     ├── ToDoListComponent.scala
│         │       │        │     ├── globalState.scala (application state)
│         │       │        │     └── model
│         │       │        │         ├── GlobalEvent.scala
│         │       │        │         └── GlobalState.scala
│         │       │        └── util  (reusable utilities)
│         │       └── test (unused)
│         └── jvm (Scala for JVM source code -- unused)
└── project
```


## Article

See [LAMINAR-REACT.md](LAMINAR-REACT.md) for a discussion of how to use Laminar in a React-like style and explanations of the various examples.
