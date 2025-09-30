package todo.util

import com.raquo.laminar.api.L.*

case class SignalRoute[A, B, +C](select: PartialFunction[A, B], map: Signal[B] => C)

case class SignalRouter[A, +C](signal: Signal[A], routes: Vector[SignalRoute[A, ?, C]]):
    private def selectPartial[B](select: PartialFunction[A, Any], result: B): PartialFunction[A, B] = {
        case (a: A) if select.isDefinedAt(a) => result
    }

    private def splitFn[C1 >: C](default: Signal[A] => C1): A => (Int | Unit) =
        val indexedRoutes = routes.zipWithIndex
        indexedRoutes.headOption match
            case None => { case _ => () }
            case Some((SignalRoute(select, _), i)) =>
                indexedRoutes.tail.foldLeft(selectPartial(select, i)) {
                    case (pf, (SignalRoute(select, _), i)) =>
                        pf.orElse(selectPartial(select, i))
                }.orElse( { case _ => () })

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

extension [A](signal: Signal[A])
    def routeSignal[B](select: PartialFunction[A, B])[C](map: Signal[B] => C): SignalRouter[A, C] =
      SignalRouter(signal, Vector(SignalRoute(select, map)))
