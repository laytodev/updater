package dev.updater.matcher.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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
                    val cls = readClass(jar.getInputStream(entry).readAllBytes()) { true }
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

        cn.methods.forEach { mn ->
            val method = MethodInstance(cls, mn.name, mn.desc, mn)
            cls.addMethod(method)
        }

        cn.fields.forEach { fn ->
            val field = FieldInstance(cls, fn.name, fn.desc, fn)
            cls.addField(field)
        }

        if(cn.superName != null) {
            addSuperClass(cls, cn.superName)
        }
        cn.interfaces.map { getCreateClass(getClassId(it)) }.forEach { itf ->
            cls.interfaces.add(itf)
            itf.implementers.add(cls)
        }
    }

    companion object {
        private fun ClassGroup.readClass(bytes: ByteArray, nameObfuscatedCheck: (ClassNode) -> Boolean): ClassInstance {
            val node = ClassNode()
            val reader = ClassReader(bytes)
            reader.accept(node, ClassReader.EXPAND_FRAMES)
            return ClassInstance(this, getClassId(node.name), node, nameObfuscatedCheck(node))
        }
    }
}