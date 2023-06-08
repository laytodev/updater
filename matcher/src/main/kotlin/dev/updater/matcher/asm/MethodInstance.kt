package dev.updater.matcher.asm

import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode

class MethodInstance(cls: ClassInstance, name: String, desc: String, val asmNode: MethodNode)
    : MemberInstance<MethodInstance>(cls, name, desc, name != "<clinit>" && name != "<init>")
{
    val args: List<ClassInstance>
    val retType: ClassInstance

    val refsIn = hashSetOf<MethodInstance>()
    val refsOut = hashSetOf<MethodInstance>()
    val classRefs = hashSetOf<ClassInstance>()

    init {
        val argTypes = Type.getArgumentTypes(desc)
        val argList = mutableListOf<ClassInstance>()

        if(argTypes.isNotEmpty()) {
            argTypes.forEach { argType ->
                val argTypeClass = group.getCreateClass(argType.descriptor)
                argList.add(argTypeClass)
                classRefs.add(argTypeClass)
                argTypeClass.methodTypeRefs.add(this)
            }
        }

        this.args = argList
        this.retType = group.getCreateClass(Type.getReturnType(desc).descriptor)
        classRefs.add(retType)
        retType.methodTypeRefs.add(this)
    }

    val id get() = "$name$desc"

    override fun isStatic() = (asmNode.access and ACC_STATIC) != 0

    override fun toString(): String {
        return "$cls.$id"
    }
}