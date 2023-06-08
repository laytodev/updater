package dev.updater.matcher.asm

abstract class MemberInstance<T : MemberInstance<T>>(
    val cls: ClassInstance,
    val name: String,
    val desc: String,
    val isNameObfuscated: Boolean
) : Matchable<T> {

    override var isMatchable = true
    override var match: T? = null

    val group get() = cls.group
    val env get() = group.env

    var parent: T? = null
    val children = hashSetOf<T>()

    abstract fun isStatic(): Boolean

    override fun toString(): String {
        return "$cls.$name"
    }
}