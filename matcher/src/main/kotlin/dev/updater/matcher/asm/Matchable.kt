package dev.updater.matcher.asm

import dev.updater.matcher.classifier.RankResult

interface Matchable<T : Matchable<T>> {

    val isNameObfuscated: Boolean
    val id: String
    val name: String

    var isMatchable: Boolean
    var match: T?

    fun hasMatch() = match != null

    val group: ClassGroup
    val env: ClassEnvironment get() = group.env

    val rankResults: MutableList<RankResult<T>>
}