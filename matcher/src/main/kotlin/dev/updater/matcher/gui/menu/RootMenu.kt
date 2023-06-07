package dev.updater.matcher.gui.menu


import org.tinylog.kotlin.Logger
import tornadofx.*
import kotlin.system.exitProcess

class RootMenu : Fragment() {

    override val root = menubar {
        menu("File") {
            item("New Project") {
                action { newAction() }
            }
            item("Open Project")
            item("Save Project")
            separator()
            item("Exit") {
                action { exitAction() }
            }
        }

        menu("View") {

        }

        menu("Matching") {
            item("Auto-Match Everything")
            item("Auto-Match Classes")
            item("Auto-Match Methods")
            item("Auto-Match Fields")
            separator()
            item("Status")
        }
    }

    private fun newAction() {

    }

    private fun exitAction() {
        Logger.info("Exiting matcher application.")
        exitProcess(0)
    }
}