package dev.updater.matcher.asm

import java.io.File

class ClassEnvironment(private val jarA: File, private val jarB: File) {

    val groupA = ClassGroup(this)
    val groupB = ClassGroup(this)
    val sharedGroup = ClassGroup(this, true)

    fun init() {
        groupA.init(jarA)
        groupB.init(jarB)
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