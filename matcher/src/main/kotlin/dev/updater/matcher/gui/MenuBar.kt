package dev.updater.matcher.gui

import dev.updater.matcher.Matcher
import tornadofx.*
import kotlin.system.exitProcess

class MenuBar : Fragment() {

    override val root = menubar {
        menu("File") {
            item("Save Mapped Jar")
            item("Save Mappings")
            separator()
            item("Reload Workspace")
            separator()
            item("Exit") {
                action { exitProcess(0) }
            }
        }

        menu("Match") {
            item("Auto-Match All") {
                action { Matcher.autoMatchAll() }
            }
            separator()
            item("Auto-Match Classes")
            item("Auto-Match Methods")
            item("Auto-Match Fields")
            separator()
            item("Matching Stats")
        }
    }
}