package dev.updater.matcher.gui.menu

import dev.updater.matcher.Config
import dev.updater.matcher.Matcher
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Parent
import javafx.scene.control.TextField
import javafx.stage.FileChooser
import org.tinylog.kotlin.Logger
import tornadofx.*
import java.io.File

class NewProjectDialog : View("New Project") {

    private val model = object : ViewModel() {
        val inputJarA = bind { SimpleStringProperty() }
        val inputJarB = bind { SimpleStringProperty() }
    }

    private lateinit var jarATextField: TextField
    private lateinit var jarBTextField: TextField

    override val root = vbox(20) {
        paddingAll = 20.0
        hbox {
            label("Jar A:") {
                paddingRight = 10.0
            }
            textfield {
                jarATextField = this
                isEditable = false
                prefWidth = 300.0
                bind(model.inputJarA)
            }
            button("Choose") {
                action {
                    chooseFile("Choose Jar A", arrayOf(FileChooser.ExtensionFilter("Jar" ,"*.jar"))).apply {
                        model.inputJarA.set(this.firstOrNull()?.absolutePath)
                    }
                }
            }
        }
        hbox {
            label("Jar B:") {
                paddingRight = 10.0
            }
            textfield {
                jarBTextField = this
                isEditable = false
                prefWidth = 300.0
                bind(model.inputJarB)
            }
            button("Choose") {
                action {
                    chooseFile("Choose Jar B", arrayOf(FileChooser.ExtensionFilter("Jar" ,"*.jar"))).apply {
                        model.inputJarB.set(this.firstOrNull()?.absolutePath)
                    }
                }
            }
        }
        button("Create Project") {
            isDisable = true
            enableWhen { model.inputJarA.isNotNull.and(model.inputJarB.isNotNull) }
            action {
                createProjectAction()
            }
        }
    }

    private fun createProjectAction() {
        Logger.info("Creating new project.")
        Config.inputJarA = File(model.inputJarA.get())
        Config.inputJarB = File(model.inputJarB.get())
        close()
        Matcher.init()
    }
}