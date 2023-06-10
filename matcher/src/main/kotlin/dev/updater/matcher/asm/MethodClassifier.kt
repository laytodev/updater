package dev.updater.matcher.asm

import dev.updater.matcher.classifier.AbstractClassifier
import dev.updater.matcher.classifier.ClassifierUtil
import dev.updater.matcher.classifier.RankResult
import org.objectweb.asm.Opcodes.*

object MethodClassifier : AbstractClassifier<MethodInstance>() {

    override fun init() {
        addClassifier(methodTypeCheck, weight = 10)
        addClassifier(accessFlags, weight = 4)
        addClassifier(argTypes, weight = 10)
        addClassifier(retType, weight = 5)
    }

    fun rank(src: MethodInstance, dsts: List<MethodInstance>): List<RankResult<MethodInstance>> {
        return ClassifierUtil.rank(src, dsts, classifiers, ClassifierUtil::isPotentiallyEqual)
    }

    private val methodTypeCheck = classifier("method type check") { a, b ->
        if(!checkAsmNodes(a, b)) return@classifier compareAsmNodes(a, b)

        val mask = ACC_STATIC or ACC_NATIVE or ACC_ABSTRACT
        val resultA = a.asmNode!!.access and mask
        val resultB = b.asmNode!!.access and mask

        return@classifier 1.0 - Integer.bitCount(resultA.xor(resultB)) / 3.0
    }

    private val accessFlags = classifier("access flags") { a, b ->
        if(!checkAsmNodes(a, b)) return@classifier compareAsmNodes(a, b)

        val mask = (ACC_PUBLIC or ACC_PROTECTED or ACC_PRIVATE) or ACC_FINAL or ACC_SYNCHRONIZED or ACC_BRIDGE or ACC_VARARGS or ACC_STRICT or ACC_SYNTHETIC
        val resultA = a.asmNode!!.access and mask
        val resultB = b.asmNode!!.access and mask

        return@classifier 1.0 - Integer.bitCount(resultA.xor(resultB)) / 8.0
    }

    private val argTypes = classifier("arg types") { a, b ->
        return@classifier ClassifierUtil.compareClassLists(a.args, b.args)
    }

    private val retType = classifier("return type") { a, b ->
        return@classifier if(ClassifierUtil.isPotentiallyEqual(a.retType, b.retType)) 1.0 else 0.0
    }

    private fun checkAsmNodes(a: MethodInstance, b: MethodInstance): Boolean {
        return a.asmNode != null && b.asmNode != null
    }

    private fun compareAsmNodes(a: MethodInstance, b: MethodInstance): Double {
        return if(a.asmNode == null && b.asmNode == null) 1.0 else 0.0
    }
}