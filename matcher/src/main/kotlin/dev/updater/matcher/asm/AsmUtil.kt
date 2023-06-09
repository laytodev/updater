package dev.updater.matcher.asm

object AsmUtil {

    fun isNameObfuscated(name: String): Boolean {
        return arrayOf("class", "method", "field").any { name.substring(name.lastIndexOf('/') + 1).startsWith(it) }
    }
}