package dev.updater.matcher.decompiler

import com.strobel.assembler.metadata.Buffer
import com.strobel.assembler.metadata.ClasspathTypeLoader
import com.strobel.assembler.metadata.CompositeTypeLoader
import com.strobel.assembler.metadata.ITypeLoader
import com.strobel.decompiler.Decompiler
import com.strobel.decompiler.DecompilerSettings
import com.strobel.decompiler.PlainTextOutput
import dev.updater.matcher.asm.ClassGroup
import dev.updater.matcher.asm.ClassInstance
import org.tinylog.kotlin.Logger


object Procyon {

    fun decompile(cls: ClassInstance, env: ClassGroup): String {
        val settings = DecompilerSettings.javaDefaults()
        settings.showSyntheticMembers = true
        settings.typeLoader = CompositeTypeLoader(
            TypeLoader(env),
            ClasspathTypeLoader()
        )
        val out = PlainTextOutput()
        Decompiler.decompile(cls.name, out, settings)
        return out.toString()
    }

    private class TypeLoader(env: ClassGroup) : ITypeLoader {

        override fun tryLoadType(internalName: String, buffer: Buffer): Boolean {
            val cls: ClassInstance? = env.getClass("L${internalName};")
            if (cls == null) {
                if (checkWarn(internalName)) {
                    //Logger.debug("Missing cls: {}", internalName)
                }
            }
            if (cls?.asmNode == null) {
                if (checkWarn(internalName)) {
                    //Logger.debug("Unknown cls: {}", internalName)
                }
            }

            val data: ByteArray = cls!!.serialize(true)
            buffer.reset(data.size)
            buffer.putByteArray(data, 0, data.size)
            buffer.position(0)
            return true
        }

        private fun checkWarn(name: String): Boolean {
            return if (name.startsWith("java/") || name.startsWith("sun/")) false else warnedClasses.add(name)
        }

        private val env: ClassGroup
        private val warnedClasses: MutableSet<String> = HashSet()

        init {
            this.env = env
        }
    }
}

