package dev.updater.matcher.gui

import dev.updater.matcher.gui.view.MainView
import javafx.stage.Stage
import tornadofx.App
import tornadofx.importStylesheet

class MatcherApp : App(MainView::class) {

    lateinit var stage: Stage private set

    init {
        INSTANCE = this
    }

    override fun start(stage: Stage) {
        super.start(stage)
        this.stage = stage
        importStylesheet("/theme.css")
    }

    companion object {
        lateinit var INSTANCE: MatcherApp private set
    }
}