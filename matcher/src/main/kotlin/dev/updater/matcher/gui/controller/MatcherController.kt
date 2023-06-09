package dev.updater.matcher.gui.controller

import dev.updater.matcher.Matcher
import dev.updater.matcher.asm.ClassInstance
import dev.updater.matcher.asm.MemberInstance
import dev.updater.matcher.gui.event.ProjectInitializeEvent
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.Controller
import tornadofx.onChange
import tornadofx.toObservable

class MatcherController : Controller() {

    val srcClasses = SimpleListProperty<ClassInstance>()
    val srcMembers = SimpleListProperty<MemberInstance<*>>()
    val srcStaticMembers = SimpleListProperty<MemberInstance<*>>()

    val srcSelectedClass = SimpleObjectProperty<ClassInstance>()
    val srcSelectedMember = SimpleObjectProperty<MemberInstance<*>>()
    val srcSelectedStaticMember = SimpleObjectProperty<MemberInstance<*>>()

    init {
        initSubscribers()
        srcSelectedClass.onChange { refreshMatchSrcLists() }
    }

    private fun initSubscribers() {
        /**
         * Project Init Event
         */
        subscribe<ProjectInitializeEvent> {
            srcClasses.clear()
            srcMembers.clear()
            srcStaticMembers.clear()
            srcClasses.set(Matcher.env.groupA.classes.sortedBy { it.name }.toObservable())
        }
    }

    private fun refreshMatchSrcLists() {
        srcMembers.clear()
        srcStaticMembers.clear()
        val cls = srcSelectedClass.get()
        if (cls != null) {
            val members = mutableListOf<MemberInstance<*>>()
            members.addAll(cls.methods.filter { !it.isStatic() })
            members.addAll(cls.fields.filter { !it.isStatic() })
            srcMembers.set(members.toObservable())

            val staticMembers = mutableListOf<MemberInstance<*>>()
            staticMembers.addAll(cls.methods.filter { it.isStatic() })
            staticMembers.addAll(cls.fields.filter { it.isStatic() })
            srcStaticMembers.set(staticMembers.toObservable())
        }
    }
}