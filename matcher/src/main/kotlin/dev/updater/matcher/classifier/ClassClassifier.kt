package dev.updater.matcher.classifier

import dev.updater.matcher.asm.ClassInstance
import org.objectweb.asm.Opcodes.*

object ClassClassifier : AbstractClassifier<ClassInstance>() {

    override fun init() {
        addClassifier(classTypeCheck, weight = 20)
    }

    private val classTypeCheck = classifier("class type check") { a, b ->
        val mask = (ACC_ENUM or ACC_INTERFACE or ACC_ANNOTATION) or ACC_ABSTRACT
        val resultA = a.asmNode!!.access and mask
        val resultB = b.asmNode!!.access and mask
        return@classifier 1.0 - Integer.bitCount(resultA.xor(resultB)) / 3.0
    }
}