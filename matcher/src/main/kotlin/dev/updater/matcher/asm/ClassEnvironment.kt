package dev.updater.matcher.asm

import org.tinylog.kotlin.Logger
import java.io.File
import java.util.concurrent.CompletableFuture

class ClassEnvironment(private val jarA: File, private val jarB: File) {

    val groupA = ClassGroup(this)
    val groupB = ClassGroup(this)
    val sharedGroup = ClassGroup(this, true)

    fun init() {
        Logger.info("Loading class environment jar files.")
        CompletableFuture.allOf(
            CompletableFuture.runAsync { groupA.init(jarA) },
            CompletableFuture.runAsync { groupB.init(jarB) }
        ).get()

        Logger.info("Processing class groups.")
        groupA.process()
        groupB.process()
    }

    fun getSharedClass(id: String) = sharedGroup.getClass(id)

    fun addSharedClass(cls: ClassInstance): ClassInstance {
        val prev = sharedGroup.addClass(cls)
        if(prev != null) return prev
        return cls
    }

}