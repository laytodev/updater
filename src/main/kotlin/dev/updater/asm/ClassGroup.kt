package dev.updater.asm

import org.objectweb.asm.tree.MethodInsnNode

class ClassGroup(val env: ClassEnv, val shared: Boolean) {

    private val classMap = hashMapOf<String, ClassInstance>()
    val classes get() = classMap.values.toList()

    fun addClass(cls: ClassInstance) {
        classMap[cls.name] = cls
    }

    fun getClass(name: String) = classMap[name]

    fun getCreateClass(name: String): ClassInstance {
        var ret = getClass(name)
        if(ret != null) return ret

        return env.getCreateClass(name)
    }

    fun process() {
        classes.forEach { processClassA(it) }
        classes.forEach { processClassB(it) }
    }

    fun processClassA(cls: ClassInstance) {
        cls.node.methods.forEach { mn ->
            val m = MethodInstance(cls, mn)
            cls.methods.add(m)
        }
        cls.node.fields.forEach { fn ->
            val f = FieldInstance(cls, fn)
            cls.fields.add(f)
        }

        if(cls.superName != null && cls.superClass == null) {
            cls.superClass = getCreateClass(cls.superName)
            cls.superClass?.children?.add(cls)
        }
        cls.interfaceNames.map { getCreateClass(it) }.forEach { itf ->
            cls.interfaces.add(itf)
            itf.implementers.add(cls)
        }
    }

    fun processClassB(cls: ClassInstance) {
        cls.methods.forEach { processMethodInsns(it) }
    }

    private fun processMethodInsns(method: MethodInstance) {
        method.instructions.forEach { insn ->
            when(insn) {
                is MethodInsnNode -> {
                    val dst = getCreateClass(insn.owner).resolveMethod(insn.name, insn.desc) ?: return@forEach
                    dst.refsIn.add(method)
                    method.refsOut.add(dst)
                    dst.cls.methodTypeRefs.add(method)
                    method.classRefs.add(dst.cls)
                }
            }
        }
    }
}