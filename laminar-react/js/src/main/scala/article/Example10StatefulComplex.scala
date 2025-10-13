package article

import com.raquo.laminar.api.L.{*, given}
import common.style.{Flex, Icon, customButton, customInput, customOption, customSelect, makeIconButton}
import org.scalajs.dom

import scala.util.Random

object StatefulComplex:
    private enum Event:
        case StartEditingItem
        case ChangeCurrentItem(newText: String)
        case StopEditingItem
        case AddCurrentItem
        case RemoveItem(index: Int)
        case Shuffle

    private case class State(currentItem: Option[String], existingItems: Vector[String]):
        def reduce(event: Event): State = event match
            case Event.StartEditingItem => copy(currentItem = currentItem.orElse(Some("")))
            case Event.ChangeCurrentItem(newText) => copy(currentItem = Some(newText))
            case Event.StopEditingItem => copy(currentItem = None)
            case Event.AddCurrentItem => copy(
                currentItem = None,
                existingItems = currentItem match {
                    case None => existingItems
                    case Some(txt) => existingItems.appended(txt)
                },
            )
            case Event.RemoveItem(index) => copy(existingItems = existingItems.patch(index, Nil, 1))
            case Event.Shuffle => copy(existingItems = scala.util.Random.shuffle(existingItems))

    def apply(): HtmlElement =
        val state = Var(State(None, Vector.empty))
        val eventSink = state.updater[Event]((state, event) => state.reduce(event))

        val addItemComponent = state.signal
            .splitMatchOne
            .handleCase({ case State(None, _) => () }) { (_, _) =>
                customButton(
                    Flex.row(Icon.add(Icon.small), "Add item", gap := "5px"),
                    onClick.mapTo(Event.StartEditingItem) --> eventSink,
                )
            }
            .handleCase({ case State(Some(txt), _) => txt }) { (_, textSignal) =>
                Flex.row(
                    "Item text",
                    customInput(value <-- textSignal, onInput.mapToValue.map(Event.ChangeCurrentItem(_)) --> eventSink),
                    Flex.row(
                        gap := "5px",
                        customButton(
                            "Add",
                            disabled <-- textSignal.map(_.strip.isEmpty),
                            onClick.mapTo(Event.AddCurrentItem) --> eventSink,
                        ),
                        customButton(
                            "Cancel",
                            onClick.mapTo(Event.StopEditingItem) --> eventSink,
                        )
                    )
                )
            }
            .toSignal

        def renderSingleListItem(itemSignal: Signal[(String, Int)]): HtmlElement = {
            val onClickMappedToRemoveItem =
                onClick(_.withCurrentValueOf(itemSignal).map(v => Event.RemoveItem(v._3)))

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
                case true => customButton("Shuffle items", onClick.mapTo(Event.Shuffle) --> eventSink)
                case false => emptyNode
        )

object Example10:
    def apply(): HtmlElement =
        StatefulComplex()
