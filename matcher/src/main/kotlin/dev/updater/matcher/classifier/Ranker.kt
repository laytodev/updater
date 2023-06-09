package dev.updater.matcher.classifier

interface Ranker<T> {
    fun rank(src: T, dsts: Collection<T>): List<Any>
}