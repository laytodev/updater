package dev.updater.matcher

import dev.updater.matcher.gui.MatcherApp
import org.tinylog.kotlin.Logger
import tornadofx.launch

object Matcher {

    @JvmStatic
    fun main(args: Array<String>) {
        launch<MatcherApp>()
    }

    fun init() {
        Logger.info("Initializing project.")
    }

}