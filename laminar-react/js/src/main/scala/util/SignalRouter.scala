package util

import com.raquo.laminar.api.L.*

case class SignalRoute[A, B, +C](select: PartialFunction[A, B], map: Signal[B] => C)

/**
 * Utility for routing signal values to avoid re-rendering elements on certain kinds of updates.
 * Each route is defined by:
 *   1. A partial function that matches cases that should not rerender and transforms them as needed
 *      to values needed for rendering these cases
 *   2. A function rendering a signal narrowed from the original values to the those transformed by (1)
 *
 * Once all cases are provided, [[result]] compiles the routes into a single signal.
 *
 * @tparam A input signal type
 * @tparam C output signal type
 */
trait SignalRouter[A, +C]:
    /**
     * Convert router to a signal, providing a backup function [[default]] to handle the input
     * signal if it does not match any of the routes.
     *
     * @param default
     *     a function using the input signal to generate a result in the case that the input
     *     signal does not match any of the routes
     * @tparam C1
     * @return
     */
    def result[C1 >: C](default: Signal[A] => C1): Signal[C1]

    /**
     * Convert router to a signal, providing a default value to use if the input signal
     * does not match any of the routes
     * @param default default value to provide if the input signal does not match any route
     */
    def result[C1 >: C](default: C1): Signal[C1]

    /**
     * Convert router to a signal, throwing a [[MatchError]] if the input signal does not match
     * any routes. For safety, use overloads with defaults.
     */
    def result[C1 >: C]: Signal[C1]

    /**
     * Add a new route, [[select]]ing cases from the original signal, then
     * transforming a signal of selected cases via [[map]]
     *
     * @param select match on one or more case from the original signal
     * @param map    transform a signal selected by [[select]]
     */
    def routeSignal[B](select: PartialFunction[A, B])[C1 >: C](map: Signal[B] => C1): SignalRouter[A, C1]

sealed case class SignalRouterImpl[A, +C](signal: Signal[A], routes: Vector[SignalRoute[A, ?, C]]) extends SignalRouter[A, C]:
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

extension [A](signal: Signal[A])
    /**
     * Transform a signal into a [[SignalRouter]], by [[select]]ing cases from the original signal and
     * transforming a signal of the selected cases via [[map]]
     *
     * @param select match on one or more case from the original signal
     * @param map transform a signal selected by [[select]]
     */
    def routeSignal[B](select: PartialFunction[A, B])[C](map: Signal[B] => C): SignalRouter[A, C] =
        SignalRouterImpl(signal, Vector(SignalRoute(select, map)))
