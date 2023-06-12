package dev.updater.matcher.gui.view

import dev.updater.matcher.asm.Matchable
import dev.updater.matcher.gui.controller.MatcherController
import tornadofx.View
import tornadofx.listview
import tornadofx.useMaxHeight

class MatchDstView : View() {

    private val controller: MatcherController by inject()

    override val root = listview<Matchable<*>>(controller.dstMatchables) {
        prefWidth = 250.0
        useMaxHeight = true
        cellFormat {
            text = it.name
        }
    }
}