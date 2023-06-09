package dev.updater.matcher.classifier

interface Classifier<T> {
    val name: String
    var weight: Double
    fun getScore(a: T, b: T): Double
}