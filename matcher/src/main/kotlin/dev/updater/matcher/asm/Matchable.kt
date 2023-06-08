package dev.updater.matcher.asm

interface Matchable<T : Matchable<T>> {

    var isMatchable: Boolean

    var match: T?

    fun hasMatch() = match != null

}