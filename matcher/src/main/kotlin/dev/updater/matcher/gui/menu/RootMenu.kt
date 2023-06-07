package dev.updater.matcher.gui.menu


import org.tinylog.kotlin.Logger
import tornadofx.*
import kotlin.system.exitProcess

class RootMenu : Fragment() {

    override val root = menubar {
        menu("File") {
            item("New Project") {
                action { newProjectAction() }
            }
            separator()
            item("Exit") {
                action { exitAction() }
            }
        }

        menu("View") {

        }

        menu("Matching") {
            item("Auto-Match All")
            separator()
            item("Auto-Match Classes")
            item("Auto-Match Methods")
            item("Auto-Match Fields")
            separator()
            item("Status")
        }
    }

    private fun newProjectAction() {
        Logger.info("Opening new project dialog.")
        NewProjectDialog().openModal(resizable = false)
    }

    private fun exitAction() {
        Logger.info("Exiting matcher application.")
        exitProcess(0)
    }
}