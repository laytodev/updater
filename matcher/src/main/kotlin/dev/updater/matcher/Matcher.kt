package dev.updater.matcher

import dev.updater.matcher.asm.ClassEnvironment
import java.io.File

object Matcher {

    lateinit var env: ClassEnvironment private set

    @JvmStatic
    fun main(args: Array<String>) {
        if(args.size < 2) error("Usage: <named jar> <deob jar>")
        val jarA = File(args[0])
        val jarB = File(args[1])
        init(jarA, jarB)
    }

    fun init(jarA: File, jarB: File) {
        env = ClassEnvironment(jarA, jarB)
        env.init()
    }
}