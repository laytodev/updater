package dev.updater.matcher.gui.view

import dev.updater.matcher.gui.menu.RootMenu
import tornadofx.View
import tornadofx.borderpane

class RootView : View("Updater") {

    override val root = borderpane {
        setPrefSize(1280.0, 900.0)
        top = find(RootMenu::class).root
    }
}