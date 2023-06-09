package dev.updater.matcher

import dev.updater.matcher.asm.ClassEnvironment
import dev.updater.matcher.asm.ClassInstance
import dev.updater.matcher.asm.FieldInstance
import dev.updater.matcher.asm.MethodInstance
import dev.updater.matcher.gui.MatcherApp
import org.tinylog.kotlin.Logger
import tornadofx.launch
import java.io.File

object Matcher {

    lateinit var env: ClassEnvironment private set

    @JvmStatic
    fun main(args: Array<String>) {
        if(args.size < 2) error("Usage: <named jar> <deob jar>")

        val jarA = File(args[0])
        val jarB = File(args[1])

        init(jarA, jarB)
        launchGui()
    }

    fun init(jarA: File, jarB: File) {
        Logger.info("Initializing.")

        env = ClassEnvironment(jarA, jarB)
        env.init()
    }

    fun launchGui() {
        Logger.info("Launching matcher GUI.")
        launch<MatcherApp>()
    }

    fun match(a: ClassInstance, b: ClassInstance) {
        if(a.match == b) return
        if(a.hasMatch()) {
            a.match!!.match = null
            unmatchMembers(a)
        }
        if(b.hasMatch()) {
            b.match!!.match = null
            unmatchMembers(b)
        }
        a.match = b
        b.match = a
        Logger.info("Matched class $a -> $b")
    }

    fun match(a: MethodInstance, b: MethodInstance) {
        if(!a.isStatic() && !b.isStatic()) {
            if(a.cls.match != b.cls) throw IllegalArgumentException("Methods are not static and dont belong to the same class.")
        }
        if(a.match == b) return
        if(a.hasMatch()) a.match!!.match = null
        if(b.hasMatch()) b.match!!.match = null
        a.match = b
        b.match = a
        Logger.info("Matched method $a -> $b")
    }

    fun match(a: FieldInstance, b: FieldInstance) {
        if(!a.isStatic() && !b.isStatic()) {
            if(a.cls.match != b.cls) throw IllegalArgumentException("Fields are not static and dont belong to the same class.")
        }
        if(a.match == b) return
        if(a.hasMatch()) a.match!!.match = null
        if(b.hasMatch()) b.match!!.match = null
        a.match = b
        b.match = a
        Logger.info("Matched field $a -> $b")
    }

    fun unmatchMembers(cls: ClassInstance) {
        cls.methods.filter { !it.isStatic() }.forEach { method ->
            if(method.hasMatch()) {
                method.match!!.match = null
                method.match = null
            }
        }
        cls.fields.filter { !it.isStatic() }.forEach { field ->
            if(field.hasMatch()) {
                field.match!!.match = null
                field.match = null
            }
        }
    }
}