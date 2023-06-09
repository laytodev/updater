package dev.updater.matcher.gui

import dev.updater.matcher.gui.view.MainView
import javafx.application.Platform
import javafx.beans.binding.BooleanExpression
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Scene
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import tornadofx.*
import java.util.concurrent.Executors
import java.util.function.Consumer
import java.util.function.DoubleConsumer
import kotlin.math.min

object ProgressUtil {

    private lateinit var dialogView: ProgressDialog

    fun runTask(title: String, task: () -> Unit) {
        Platform.runLater {
            dialogView = ProgressDialog()
            dialogView.label.set(title)
            dialogView.openModal(StageStyle.UTILITY, Modality.APPLICATION_MODAL, resizable = false)
            dialogView.modalStage!!.setOnCloseRequest { it.consume() }
            dialogView.modalStage!!.requestFocus()
            Executors.newSingleThreadExecutor().execute(task)
        }
    }

    fun setProgress(value: Double) {
        dialogView.progress.set(value)
    }

    fun addProgress(value: Double) {
        dialogView.progress.set(min(dialogView.progress.get() + value, 1.0))
    }

    fun close() {
        Platform.runLater { dialogView.close() }
    }

    private class ProgressDialog : View("Operation Progress") {

        val progress = SimpleDoubleProperty(0.1)
        val label = SimpleStringProperty("")

        init {
            progress.onChange {
                if(it >= 1.0) Platform.runLater { close() }
            }
        }

        override val root = vbox {
            paddingAll = 5.0
            label {
                paddingBottom = 5.0
                textProperty().bindBidirectional(label)
            }
            progressbar(progress) {
                prefWidth = 350.0
                prefHeight = 18.0
                usePrefSize = true
            }
        }
    }
}