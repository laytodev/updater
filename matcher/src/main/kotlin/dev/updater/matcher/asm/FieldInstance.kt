package dev.updater.matcher.asm

import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.tree.FieldNode

class FieldInstance(cls: ClassInstance, name: String, desc: String, val asmNode: FieldNode)
    : MemberInstance<FieldInstance>(cls, name, desc, true)
{

    val type: ClassInstance = group.getCreateClass(desc).also {
        it.fieldTypeRefs.add(this)
    }

    val id get() = "$name:$desc"

    val readRefs = hashSetOf<MethodInstance>()
    val writeRefs = hashSetOf<MethodInstance>()

    override fun isStatic() = (asmNode.access and ACC_STATIC) != 0

    override fun toString(): String {
        return "$cls.$name"
    }
}