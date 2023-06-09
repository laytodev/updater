package dev.updater.matcher.gui.view

import dev.updater.matcher.gui.MenuBar
import tornadofx.*

class MainView : View("Matcher") {

    override val root = borderpane {
        setPrefSize(1280.0, 1000.0)
        top = find(MenuBar::class).root
    }

}