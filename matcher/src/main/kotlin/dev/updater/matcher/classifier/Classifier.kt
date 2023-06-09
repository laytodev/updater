package dev.updater.matcher.classifier

interface Classifier<T> {
    val name: String
    val weight: Double
    fun getScore(a: T, b: T, )
}