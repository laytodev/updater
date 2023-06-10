package dev.updater.matcher.classifier

interface Ranker<T> {
    fun rank(src: T, dsts: List<T>): List<RankResult<T>>
}