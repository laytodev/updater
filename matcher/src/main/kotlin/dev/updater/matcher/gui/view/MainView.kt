package dev.updater.matcher.gui.view

import dev.updater.matcher.gui.MenuBar
import dev.updater.matcher.gui.event.ProjectInitializeEvent
import tornadofx.*

class MainView : View("Matcher") {

    override val root = borderpane {
        setPrefSize(1500.0, 1000.0)
        top = find(MenuBar::class).root
        left = find(MatchSrcView::class).root
        right = find(MatchDstView::class).root
    }

    override fun onDock() {
        fire(ProjectInitializeEvent())
    }
}