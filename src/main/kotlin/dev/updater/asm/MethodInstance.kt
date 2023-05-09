package dev.updater.asm

import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode

class MethodInstance(val cls: ClassInstance, val node: MethodNode) : Matchable<MethodInstance> {

    override var matchable = true
    override var match: MethodInstance? = null

    val group get() = cls.group
    val env get() = group.env

    val access = node.access
    val name = node.name
    val desc = node.desc
    val tryCatchBlocks = node.tryCatchBlocks
    val instructions = node.instructions
    val parameters = node.parameters
    val locals = node.localVariables

    val refsIn = hashSetOf<MethodInstance>()
    val refsOut = hashSetOf<MethodInstance>()
    val fieldReadRefs = hashSetOf<FieldInstance>()
    val fieldWriteRefs = hashSetOf<FieldInstance>()
    val classRefs = hashSetOf<ClassInstance>()

    val type = Type.getMethodType(desc)

    val argTypes = hashSetOf<ClassInstance>().also {
        it.addAll(type.argumentTypes.map { group.getCreateClass(it.internalName) })
    }
    val returnType = group.getCreateClass(type.returnType.internalName)

    override fun toString(): String {
        return "$cls.$name$desc"
    }
}