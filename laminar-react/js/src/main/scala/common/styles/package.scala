package common

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveElement
import org.scalajs.dom.{HTMLImageElement, HTMLOptionElement, HTMLSelectElement}

package object styles:

    val makeButton: Modifier[HtmlElement] = Seq(
        className := "standard-button",
    )

    val makeIconButton: Modifier[HtmlElement] = Seq(
        className := "icon-button"
    )

    def card(modifiers: Modifier[HtmlElement]*): HtmlElement =
        div(
            borderRadius := "15px",
            padding := "20px",
            boxShadow := "0 4px 8px 0 rgba(0,0,0,0.2)",
            modifiers,
        )

    def customSelect(modifiers: Modifier[HtmlElement]*): HtmlElement = {
        div(
            position.relative,
            select(
                width := "100%",
                fontSize.inherit,
                padding := "12px",
                paddingRight := "50px",
                cursor.pointer,
                borderWidth := "1px",
                modifiers,
            ),
            // Overriding the dropdown icon is a little complicated!
            div(
                pointerEvents.none,
                position.absolute,
                right := "0",
                top := "20%",
                bottom := "20%",
                width := "100%",
                Icon.chevronDown(
                    pointerEvents.none,
                    position.absolute,
                    right := "0",
                    height := "100%",
                    width := "50px",
                ),
            ),
        )

    }

    def customButton(modifiers: Modifier[HtmlElement]*): HtmlElement =
        button(
            makeButton,
            modifiers,
        )

    def customOption(modifiers: Modifier[HtmlElement]*): ReactiveElement[HTMLOptionElement] =
        option(
            modifiers,
        )

    def customInput(modifiers: Modifier[HtmlElement]*): HtmlElement =
        input(
            fontSize.inherit,
            backgroundColor.inherit,
            padding := "12px",
            borderWidth := "1px",
            modifiers,
        )

    object Flex:
        val cluster: Modifier[HtmlElement] = Seq(
            justifyContent := "flex-start"
        )

        val split: Modifier[HtmlElement] = Seq(
            justifyContent := "space-between"
        )

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

    object Icon:
        val small: Modifier[HtmlElement] = Seq(
            height := "1rem",
            width := "1rem",
        )

        val medium: Modifier[HtmlElement] = Seq(
            height := "2rem",
            width := "2rem",
        )

        val large: Modifier[HtmlElement] = Seq(
            height := "3rem",
            width := "3rem",
        )

        private def iconSrc(iconName: String): Modifier[HtmlElement] =
            src := s"https://cdn.jsdelivr.net/npm/@tabler/icons@latest/icons/$iconName.svg"

        private def icon(iconName: String)(modifiers: Modifier[HtmlElement]*): HtmlElement = {
            div(
                medium,
                borderRadius := "50%",
                display.flex,
                flexDirection.column,
                alignItems.center,
                justifyContent.center,
                img(
                    margin := "5px",
                    height := "75%",
                    iconSrc(iconName),
                ),
                modifiers,
            )
        }

        def chevronRight(modifiers: Modifier[HtmlElement]*): HtmlElement =
            icon("chevron-right")(modifiers)

        def chevronDown(modifiers: Modifier[HtmlElement]*): HtmlElement =
            icon("chevron-down")(modifiers)

        def close(modifiers: Modifier[HtmlElement]*): HtmlElement =
            icon("x")(modifiers)

        def plus(modifiers: Modifier[HtmlElement]*): HtmlElement =
            icon("plus")(modifiers)

        def refresh(modifiers: Modifier[HtmlElement]*): HtmlElement =
            icon("refresh")(modifiers)
