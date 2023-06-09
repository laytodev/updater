package dev.updater.matcher.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes.GETFIELD
import org.objectweb.asm.Opcodes.GETSTATIC
import org.objectweb.asm.tree.AbstractInsnNode.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayDeque
import java.util.jar.JarFile

class ClassGroup(val env: ClassEnvironment, val isShared: Boolean = false) {

    private val classMap = hashMapOf<String, ClassInstance>()
    val classes get() = classMap.values.toList()

    private val arrayClassMap = hashMapOf<String, ClassInstance>()
    val arrayClasses get() = arrayClassMap.values.toList()

    fun init(file: File) {
        JarFile(file).use { jar ->
            jar.entries().asSequence().forEach { entry ->
                if(entry.name.endsWith(".class")) {
                    val cls = readClass(jar.getInputStream(entry).readAllBytes()) { AsmUtil.isNameObfuscated(it.name) }
                    classMap.putIfAbsent(cls.id, cls)
                }
            }
        }
    }

    fun process() {
        val initialClasses = mutableListOf<ClassInstance>().also {
            it.addAll(classes)
        }

        initialClasses.forEach { cls ->
            processClassA(cls)
        }

        initialClasses.forEach { cls ->
            processClassB(cls)
        }
    }

    fun addClass(cls: ClassInstance): ClassInstance? {
        return classMap.putIfAbsent(cls.id, cls)
    }

    fun getClass(name: String) = if(name[0] == '[') {
        arrayClassMap[name]
    } else {
        classMap[getClassId(name)]
    }

    fun getCreateClass(id: String): ClassInstance {
        var ret: ClassInstance?

        if(id[0] == '[') {
            ret = env.getSharedClass(id)
            if(ret != null) return ret

            ret = arrayClassMap[id]
            if(ret != null) return ret

            val elementId = id.substring(id.lastIndexOf('[') + 1)
            val elementClass = getCreateClass(elementId)
            val cls = ClassInstance(env.sharedGroup, id, elementClass)

            if(elementClass.isShared()) {
                ret = env.addSharedClass(cls)
            } else {
                ret = arrayClassMap.putIfAbsent(id, cls)
                if(ret == null) { ret = cls }
            }

            if(ret == cls) {
                addSuperClass(ret, "java/lang/Object")
            }
        } else {
            ret = classMap[id]
            if(ret != null) return ret

            ret = env.getSharedClass(id)
            if(ret != null) return ret

            ret = getMissingClass(id)
        }

        return ret
    }

    private fun getClassId(name: String): String {
        return if(name[0] == '[') {
            name
        } else {
            "L$name;"
        }
    }

    private fun addSuperClass(cls: ClassInstance, name: String) {
        cls.superClass = getCreateClass(getClassId(name))
        cls.superClass?.children?.add(cls)
    }

    private fun getMissingClass(id: String): ClassInstance {
        if(id.length > 1) {
            val name = id.substring(1, id.length - 1)
            var file: Path? = null
            val url = ClassLoader.getSystemResource("$name.class")
            if(url != null) {
                file = getPath(url)
            }

            if(file != null) {
                val cls = env.sharedGroup.readClass(Files.readAllBytes(file)) { false }
                cls.match = cls

                val ret = env.addSharedClass(cls)
                if(ret == cls) {
                    processClassA(ret)
                }

                return ret
            }
        }

        val ret = ClassInstance(env.sharedGroup, id)
        env.addSharedClass(ret)

        return ret
    }

    private fun getPath(url: URL): Path {
        val uri = url.toURI()
        var ret = Paths.get(uri)
        if(uri.scheme == "jrt" && !Files.exists(ret)) {
            ret = Paths.get(URI(uri.scheme, uri.userInfo, uri.host, uri.port, "/modules".plus(uri.path), uri.query, uri.fragment))
        }
        return ret
    }

    private fun processClassA(cls: ClassInstance) {
        val cn = cls.asmNode!!

        // Add methods and fields to class
        cn.methods.forEach { mn ->
            val method = MethodInstance(cls, mn.name, mn.desc, mn)
            cls.addMethod(method)
        }

        cn.fields.forEach { fn ->
            val field = FieldInstance(cls, fn.name, fn.desc, fn)
            cls.addField(field)
        }

        // Add / Set outer and inner classes
        if(cn.outerClass != null) {
            addOuterClass(cls, cn.outerClass)
        } else {
            cn.innerClasses.forEach { inCls ->
                if(inCls.name == cn.name) {
                    addOuterClass(cls, inCls.outerName)
                    return
                }
            }
            var pos: Int
            if(cn.name.lastIndexOf('$').also { pos = it } != -1 && pos < cn.name.length - 1) {
                addOuterClass(cls, cn.name.substring(0, pos))
            }
        }

        if(cn.superName != null) {
            addSuperClass(cls, cn.superName)
        }
        cn.interfaces.map { getCreateClass(getClassId(it)) }.forEach { itf ->
            cls.interfaces.add(itf)
            itf.implementers.add(cls)
        }
    }

    private fun processClassB(cls: ClassInstance) {
        val queue = ArrayDeque<ClassInstance>()
        val visited = hashSetOf<ClassInstance>()

        cls.methods.forEach { method ->
            processMethod(method, queue, visited)
            queue.clear()
            visited.clear()
        }

        cls.fields.forEach { field ->
            processField(field, queue, visited)
            queue.clear()
            visited.clear()
        }
    }

    private fun addOuterClass(cls: ClassInstance, name: String) {
        var outerClass = getClass(name)
        if(outerClass == null) {
            outerClass = getCreateClass(getClassId(name))
        }

        cls.outerClass = outerClass
        outerClass.innerClasses.add(cls)
    }

    private fun processMethod(method: MethodInstance, queue: ArrayDeque<ClassInstance>, visited: HashSet<ClassInstance>) {
        if(method.cls.superClass != null) queue.add(method.cls.superClass!!)
        queue.addAll(method.cls.interfaces)

        var cls: ClassInstance?
        while(queue.poll().also { cls = it } != null) {
            if(!visited.add(cls!!)) continue

            val m = cls!!.getMethod(method.name, method.desc)
            if(m != null) {
                method.parent = m
                m.children.add(method)
            } else {
                if(cls!!.superClass != null) queue.add(cls!!.superClass!!)
                queue.addAll(cls!!.interfaces)
            }
        }

        if(method.asmNode == null) {
            return
        }

        val insns = method.asmNode.instructions.iterator()
        while(insns.hasNext()) {
            val insn = insns.next()

            when(insn.type) {
                METHOD_INSN -> {
                    insn as MethodInsnNode
                    val owner = getCreateClass(getClassId(insn.owner))
                    var dst = owner.resolveMethod(insn.name, insn.desc)

                    if(dst == null) {
                        dst = MethodInstance(owner, insn.name, insn.desc, null)
                        owner.addMethod(dst)
                    }

                    dst.refsIn.add(method)
                    method.refsOut.add(dst)
                    dst.cls.methodTypeRefs.add(method)
                    method.classRefs.add(dst.cls)
                }

                FIELD_INSN -> {
                    insn as FieldInsnNode
                    val owner = getCreateClass(getClassId(insn.owner))
                    var dst = owner.resolveField(insn.name, insn.desc)

                    if(dst == null) {
                        dst = FieldInstance(owner, insn.name, insn.desc, null)
                        owner.addField(dst)
                    }

                    if(insn.opcode == GETSTATIC || insn.opcode == GETFIELD) {
                        dst.readRefs.add(method)
                        method.fieldReadRefs.add(dst)
                    } else {
                        dst.writeRefs.add(method)
                        method.fieldWriteRefs.add(dst)
                    }

                    dst.cls.methodTypeRefs.add(method)
                    method.classRefs.add(dst.cls)
                }

                TYPE_INSN -> {
                    insn as TypeInsnNode
                    val dst = getCreateClass(getClassId(insn.desc))
                    dst.methodTypeRefs.add(method)
                    method.classRefs.add(dst)
                }
            }
        }
    }

    private fun processField(field: FieldInstance, queue: ArrayDeque<ClassInstance>, visited: HashSet<ClassInstance>) {
        if(field.cls.superClass != null) queue.add(field.cls.superClass!!)
        queue.addAll(field.cls.interfaces)

        var cls: ClassInstance?
        while(queue.poll().also { cls = it } != null) {
            if(!visited.add(cls!!)) continue

            val f = cls!!.getField(field.name, field.desc)

            if(f != null) {
                field.parent = f
                f.children.add(field)
            } else {
                if(cls!!.superClass != null) queue.add(cls!!.superClass!!)
                queue.addAll(cls!!.interfaces)
            }
        }
    }

    companion object {
        private fun ClassGroup.readClass(bytes: ByteArray, nameObfuscatedCheck: (ClassNode) -> Boolean): ClassInstance {
            val node = ClassNode()
            val reader = ClassReader(bytes)
            reader.accept(node, ClassReader.SKIP_FRAMES)
            return ClassInstance(this, getClassId(node.name), node, nameObfuscatedCheck(node))
        }
    }
}