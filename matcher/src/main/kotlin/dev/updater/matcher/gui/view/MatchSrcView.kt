package dev.updater.matcher.gui.view

import dev.updater.matcher.asm.ClassInstance
import dev.updater.matcher.asm.MemberInstance
import dev.updater.matcher.gui.controller.MatcherController
import javafx.geometry.Orientation
import javafx.scene.control.ListCell
import javafx.scene.control.SelectionMode
import javafx.scene.paint.Color
import tornadofx.*

class MatchSrcView : View() {

    private val controller: MatcherController by inject()

    val classList = listview<ClassInstance>(controller.srcClasses) {
        prefHeight = 500.0
        usePrefHeight = true
        cellFormat {
            text = it.toString()
            textFill = if(it.hasMatch()) Color.DARKGREEN else Color.DARKGRAY
        }
        selectionModel.selectionMode = SelectionMode.SINGLE
        bindSelected(controller.srcSelectedClass)
    }

    val memberList = listview<MemberInstance<*>>(controller.srcMembers) {
        prefHeight = 250.0
        usePrefHeight = true
        cellFormat {
            text = it.toString()
            textFill = if(it.hasMatch()) Color.DARKGREEN else Color.DARKGRAY
        }
        selectionModel.selectionMode = SelectionMode.SINGLE
        bindSelected(controller.srcSelectedMember)
    }

    val staticMemberList = listview<MemberInstance<*>>(controller.srcStaticMembers) {
        useMaxHeight = true
        cellFormat {
            text = it.toString()
            textFill = if(it.hasMatch()) Color.DARKGREEN else Color.DARKGRAY
        }
        selectionModel.selectionMode = SelectionMode.SINGLE
        bindSelected(controller.srcSelectedStaticMember)
    }

    override val root = splitpane(Orientation.VERTICAL) {
        prefWidth = 200.0
        useMaxHeight = true
        items.addAll(
            classList,
            memberList,
            staticMemberList
        )
    }
    
    private fun ListCell<*>.applyMatchColors(cls: ClassInstance) {
        var matchLevel = 1
        if(cls.hasMatch()) {
            matchLevel++
        }
        if(cls.methods.any { cls.hasMatch() } || cls.fields.any { cls.hasMatch() }) {
            matchLevel++
        }
        when(matchLevel) {
            0 -> styleClass.add("no-match-cell")
            1 -> styleClass.add("low-match-similarity-cell")
            2 -> styleClass.add("moderate-match-similarity-cell")
            3 -> styleClass.add("high-match-similarity-cell")
        }
    }

    private fun ListCell<*>.applyMatchColors(member: MemberInstance<*>) {
        var matchLevel = 1
        if(member.hasMatch()) {
            matchLevel = 3
        }
        when(matchLevel) {
            0 -> styleClass.add("no-match-cell")
            1 -> styleClass.add("low-match-similarity-cell")
            2 -> styleClass.add("moderate-match-similarity-cell")
            3 -> styleClass.add("high-match-similarity-cell")
        }
    }
}