package dev.updater.matcher.asm

import dev.updater.matcher.classifier.RankResult

abstract class MemberInstance<T : MemberInstance<T>>(
    val cls: ClassInstance,
    override val name: String,
    val desc: String,
    override val isNameObfuscated: Boolean
) : Matchable<T> {

    override var isMatchable = true
    override var match: T? = null

    override val group get() = cls.group
    override val rankResults = mutableListOf<RankResult<T>>()

    var parent: T? = null
    val children = hashSetOf<T>()

    abstract fun isStatic(): Boolean

    override fun toString(): String {
        return "$cls.$name"
    }
}