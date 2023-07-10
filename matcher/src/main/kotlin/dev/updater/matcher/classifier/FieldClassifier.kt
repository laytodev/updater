package dev.updater.matcher.classifier

import dev.updater.matcher.asm.ClassInstance
import dev.updater.matcher.asm.FieldInstance
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.FieldInsnNode

object FieldClassifier : AbstractClassifier<FieldInstance>() {

    override fun init() {
        addClassifier(fieldTypeCheck, weight = 10)
        addClassifier(accessFlags, weight = 4)
        addClassifier(type, weight = 10)
    }

    fun rank(src: FieldInstance, dsts: List<FieldInstance>, maxMismatch: Double): List<RankResult<FieldInstance>> {
        return ClassifierUtil.rank(src, dsts, classifiers, Double.POSITIVE_INFINITY, ClassifierUtil::isPotentiallyEqual)
    }

    private val fieldTypeCheck = classifier("field type check") { a, b ->
        if(!checkAsmNodes(a, b)) return@classifier compareAsmNodes(a, b)

        val mask = ACC_STATIC
        val resultA = a.asmNode!!.access and mask
        val resultB = b.asmNode!!.access and mask

        return@classifier 1.0 - Integer.bitCount(resultA.xor(resultB)) / 1.0
    }

    private val accessFlags = classifier("access flags") { a, b ->
        if(!checkAsmNodes(a, b)) return@classifier compareAsmNodes(a, b)

        val mask = (ACC_PUBLIC or ACC_PROTECTED or ACC_PRIVATE) or ACC_FINAL or ACC_VOLATILE or ACC_TRANSIENT or ACC_SYNTHETIC
        val resultA = a.asmNode!!.access and mask
        val resultB = b.asmNode!!.access and mask
        return@classifier 1.0 - Integer.bitCount(resultA.xor(resultB)) / 6.0
    }

    private val type = classifier("type") { a, b ->
        return@classifier if(ClassifierUtil.isPotentiallyEqual(a.type, b.type)) 1.0 else 0.0
    }


    private fun checkAsmNodes(a: FieldInstance, b: FieldInstance): Boolean {
        return a.asmNode != null && b.asmNode != null
    }

    private fun compareAsmNodes(a: FieldInstance, b: FieldInstance): Double {
        return if(a.asmNode == null && b.asmNode == null)  1.0 else 0.0
    }

    private fun isSameField(insn: FieldInsnNode, owner: String, name: String, desc: String, field: FieldInstance): Boolean {
        var target: ClassInstance? = null
        return insn.name == name
                && insn.desc == desc
                && (insn.owner == owner) || (field.group.getClass(insn.owner)?.also { target = it } != null && target!!.resolveField(name, desc) == field)
    }
}