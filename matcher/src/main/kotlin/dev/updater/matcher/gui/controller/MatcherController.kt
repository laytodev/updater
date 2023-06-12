package dev.updater.matcher.gui.controller

import dev.updater.matcher.Matcher
import dev.updater.matcher.asm.ClassInstance
import dev.updater.matcher.asm.Matchable
import dev.updater.matcher.asm.MemberInstance
import dev.updater.matcher.gui.event.ProjectInitializeEvent
import dev.updater.matcher.gui.event.SelectedSrcMatchableEvent
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.Controller
import tornadofx.onChange
import tornadofx.toObservable

class MatcherController : Controller() {

    val srcClasses = SimpleListProperty<ClassInstance>()
    val srcMembers = SimpleListProperty<MemberInstance<*>>()
    val srcStaticMembers = SimpleListProperty<MemberInstance<*>>()

    val dstMatchables = SimpleListProperty<Matchable<*>>()

    val srcSelectedClass = SimpleObjectProperty<ClassInstance>()
    val srcSelectedMember = SimpleObjectProperty<MemberInstance<*>>()
    val srcSelectedStaticMember = SimpleObjectProperty<MemberInstance<*>>()

    init {
        initSubscribers()
        srcSelectedClass.onChange {
            refreshMatchSrcLists()
            if(it != null && it.hasMatch()) {
                fire(SelectedSrcMatchableEvent(it))
            } else {
                dstMatchables.set(listOf<Matchable<*>>().toObservable())
            }
        }
        srcSelectedMember.onChange {
            if(it != null && it.hasMatch()) {
                fire(SelectedSrcMatchableEvent(it))
            } else {
                fire(SelectedSrcMatchableEvent(srcSelectedClass.get()))
            }
        }
        srcSelectedStaticMember.onChange {
            if(it != null && it.hasMatch()) {
                fire(SelectedSrcMatchableEvent(it))
            } else {
                fire(SelectedSrcMatchableEvent(srcSelectedClass.get()))
            }
        }
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

        /**
         * Selected Src Matchable Changed Event
         */
        subscribe<SelectedSrcMatchableEvent> {
            dstMatchables.clear()
            if(it.src.hasMatch()) {
                dstMatchables.set(listOf(it.src.match).toObservable())
            }
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