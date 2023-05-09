package dev.updater.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.TypePath
import org.objectweb.asm.TypeReference
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile

class ClassEnv {

    val groupA = ClassGroup(this, false)
    val groupB = ClassGroup(this, false)
    val sharedGroup = ClassGroup(this, true)

    fun init(jarA: File, jarB: File) {
        readJar(jarA, groupA)
        readJar(jarB, groupB)
        groupA.process()
        groupB.process()
    }

    fun readClass(bytes: ByteArray): ClassNode {
        val node = ClassNode()
        val reader = ClassReader(bytes)
        reader.accept(node, ClassReader.EXPAND_FRAMES)
        return node
    }

    fun readJar(file: File, group: ClassGroup) {
        JarFile(file).use { jar ->
            jar.entries().asSequence().forEach { entry ->
                if(entry.name.endsWith(".class")) {
                    val node = readClass(jar.getInputStream(entry).readAllBytes())
                    val cls = ClassInstance(group, node, true)
                    group.addClass(cls)
                }
            }
        }
    }

    fun addSharedClass(cls: ClassInstance) {
        sharedGroup.addClass(cls)
    }

    fun getSharedClass(name: String) = sharedGroup.getClass(name)

    fun getCreateClass(name: String): ClassInstance {
        var ret = getSharedClass(name)
        if(ret != null) return ret

        val clsName = if(name.length == 1) Type.getType(name).className else name

        ret = getMissingClass(clsName)

        return ret
    }

    private fun getMissingClass(name: String): ClassInstance {
        var file: Path? = null
        val url = ClassLoader.getSystemResource("$name.class")
        if(url != null) {
            val uri = url.toURI()
            var ret = Paths.get(uri)
            if(uri.scheme == "jrt" && !Files.exists(ret)) {
                ret = Paths.get(URI(uri.scheme, uri.userInfo, uri.host, uri.port, "/modules".plus(uri.path), uri.query, uri.fragment))
            }
            file = ret
        }
        if(file != null) {
            val node = readClass(Files.readAllBytes(file))
            val cls = ClassInstance(sharedGroup, node, false)
            if(getSharedClass(cls.name) == null) {
                addSharedClass(cls)
                sharedGroup.processClassA(cls)
            }
            return cls
        }

        val node = ClassNode()
        node.version = V1_8
        node.access = ACC_PUBLIC or ACC_SUPER
        node.name = name
        node.superName = "java/lang/Object"
        node.interfaces = emptyList()
        val cls = ClassInstance(sharedGroup, node, false)
        addSharedClass(cls)

        return cls
    }
}