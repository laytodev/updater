package dev.updater.matcher.gui.view

import javafx.concurrent.Worker
import javafx.geometry.Orientation
import tornadofx.*
import java.util.ArrayDeque

class SourceCodeView : View() {

    private var template: String = ""
    private val taskQueue = ArrayDeque<() -> Unit>()

    val codeView= webview {
        text("source")
    }

    override val root = codeView

    fun runTask(block: () -> Unit) {
        if(codeView.engine.loadWorker.state == Worker.State.SUCCEEDED) {
            block()
        } else {
            taskQueue.add(block)
        }
    }

    fun scrollToTop(value: Double) {
        runTask { codeView.engine.executeScript("document.body.scrollTop = $value") }
    }

    fun displayHtml(html: String) {
        val h = template
            .replace("%text%", html)
            .replace("%theme%", SourceCodeView::class.java.getResource("/theme.css")!!.toURI().toURL().toExternalForm())
        println(h)
        codeView.engine.loadContent(h)
    }

    private fun readTemplate(): String {
        val bytes = SourceCodeView::class.java.getResourceAsStream("/html/CodeView.htm")!!.readAllBytes()
        return bytes.toString()
    }

    override fun onDock() {
        template = readTemplate()
        codeView.engine.loadWorker.stateProperty().addListener { ob, old, new ->
            if(new == Worker.State.SUCCEEDED) {
                var r: () -> Unit
                while(taskQueue.poll().also { r = it } != null) {
                    r.invoke()
                }
            }
        }
    }
}