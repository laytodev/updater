package dev.updater.matcher.classifier

import dev.updater.matcher.asm.Matchable

abstract class AbstractClassifier<T : Matchable<T>> {

    val classifiers = hashMapOf<Classifier<T>, Double>()
    var maxScore = 0.0

    abstract fun init()

    fun addClassifier(classifier: Classifier<T>, weight: Int) {
        classifiers[classifier] = weight.toDouble()
        maxScore += weight.toDouble()
    }

    @DslMarker
    annotation class ClassifierDslMarker

    @ClassifierDslMarker
    fun classifier(name: String, block: (a: T, b: T) -> Double): Classifier<T> {
        return object : Classifier<T> {
            override val name = name
            override var weight = 0.0
            override fun getScore(a: T, b: T): Double {
                return block(a, b)
            }
        }
    }
}