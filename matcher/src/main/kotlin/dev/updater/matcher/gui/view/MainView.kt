package dev.updater.matcher.gui.view

import dev.updater.matcher.decompiler.Procyon
import dev.updater.matcher.gui.MenuBar
import dev.updater.matcher.gui.controller.MatcherController
import dev.updater.matcher.gui.event.ProjectInitializeEvent
import javafx.geometry.Orientation
import tornadofx.*

class MainView : View("Matcher") {

    private val controller: MatcherController by inject()

    val srcCodeView = SourceCodeView()
    val dstCodeView = SourceCodeView()

    override val root = borderpane {
        setPrefSize(1500.0, 1000.0)
        top = find(MenuBar::class).root
        left = find(MatchSrcView::class).root
        right = find(MatchDstView::class).root
        center = splitpane(Orientation.VERTICAL) {
            add(srcCodeView.root)
            add(dstCodeView.root)
        }
    }

    override fun onDock() {
        fire(ProjectInitializeEvent())

        controller.srcSelectedClass.onChange {
            val srcCode = if (it != null) {
                Procyon.decompile(it, it.group)
            } else {
                ""
            }
            srcCodeView.displayHtml(srcCode)

            val dstCode = if (it?.match != null) {
                Procyon.decompile(it.match!!, it.match!!.group)
            } else {
                ""
            }
            dstCodeView.displayHtml(dstCode)
        }
    }
}