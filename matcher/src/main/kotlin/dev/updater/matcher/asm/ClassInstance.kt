package dev.updater.matcher.asm

import org.objectweb.asm.Opcodes.ACC_INTERFACE
import org.objectweb.asm.tree.ClassNode
import java.util.ArrayDeque

class ClassInstance private constructor(
    val group: ClassGroup,
    val id: String,
    val asmNode: ClassNode?,
    val elementClass: ClassInstance?,
    val isNameObfuscated: Boolean
) : Matchable<ClassInstance> {

    override var isMatchable = true
    override var match: ClassInstance? = null

    constructor(group: ClassGroup, id: String, asmNode: ClassNode, isNameObfuscated: Boolean) : this(group, id, asmNode, null, isNameObfuscated)

    constructor(group: ClassGroup, id: String) : this(group, id, null, null, false) {
        match = this
    }

    constructor(group: ClassGroup, id: String, elementClass: ClassInstance) : this(group, id, null, elementClass, false) {
        if(elementClass.isShared()) {
            match = this
        }
    }

    val name: String get() = if(id.startsWith("L")) id.substring(1, id.length - 1) else id

    var superClass: ClassInstance? = null
    val children = hashSetOf<ClassInstance>()
    val interfaces = hashSetOf<ClassInstance>()
    val implementers = hashSetOf<ClassInstance>()
    val methodTypeRefs = hashSetOf<MethodInstance>()
    val fieldTypeRefs = hashSetOf<FieldInstance>()

    var outerClass: ClassInstance? = null
    val innerClasses = hashSetOf<ClassInstance>()

    private val methodMap = hashMapOf<String, MethodInstance>()
    val methods get() = methodMap.values.toList()

    private val fieldMap = hashMapOf<String, FieldInstance>()
    val fields get() = fieldMap.values.toList()

    fun isPrimitive() = id[0] != 'L' && id[0] != '['
    fun isArray() = elementClass != null
    fun isShared() = group.isShared

    fun isInterface(): Boolean {
        return if(asmNode != null) {
            (asmNode.access and ACC_INTERFACE) != 0
        } else {
            implementers.isNotEmpty()
        }
    }

    fun addMethod(method: MethodInstance) {
        methodMap[method.id] = method
    }

    fun addField(field: FieldInstance) {
        fieldMap[field.id] = field
    }

    fun getMethod(name: String, desc: String) = methodMap["$name$desc"]

    fun getField(name: String, desc: String) = fieldMap["$name:$desc"]

    fun resolveMethod(name: String, desc: String): MethodInstance? {
        var cls: ClassInstance? = this

        do {
            val ret = cls!!.getMethod(name, desc)
            if(ret != null) return ret
            cls = cls.superClass
        } while(cls != null)

        if(isInterface()) {
            if(interfaces.isEmpty()) return null

            val queue = ArrayDeque<ClassInstance>()
            queue.addAll(interfaces)

            while(queue.poll().also { cls = it } != null) {
                val ret = cls!!.getMethod(name, desc)
                if(ret != null) return ret
                queue.addAll(cls!!.interfaces)
            }
        }

        return null
    }



    fun resolveField(name: String, desc: String): FieldInstance? {
        var ret = getField(name, desc)
        if(ret != null) return ret

        if(interfaces.isNotEmpty()) {
            val queue = ArrayDeque<ClassInstance>()
            queue.addAll(interfaces)

            var cls: ClassInstance
            while(queue.pollFirst().also { cls = it } != null) {
                ret = cls.getField(name, desc)
                if(ret != null) return ret
                cls.interfaces.forEach { queue.addFirst(it) }
            }
        }

        var cls = superClass
        while(cls != null) {
            ret = cls!!.getField(name, desc)
            if(ret != null) return ret
            cls = cls!!.superClass
        }

        return null
    }

    override fun toString(): String {
        return name
    }
}