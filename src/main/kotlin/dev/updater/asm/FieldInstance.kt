package dev.updater.asm

import org.objectweb.asm.Type
import org.objectweb.asm.tree.FieldNode

class FieldInstance(val cls: ClassInstance, val node: FieldNode) : Matchable<FieldInstance> {

    override var matchable = true
    override var match: FieldInstance? = null

    val group get() = cls.group
    val env get() = group.env

    val access = node.access
    val name = node.name
    val desc = node.desc
    val value = node.value

    val readRefs = hashSetOf<MethodInstance>()
    val writeRefs = hashSetOf<MethodInstance>()

    val type = Type.getType(desc)
    val typeClass = group.getCreateClass(type.internalName)

    override fun toString(): String {
        return "$cls.$name"
    }
}