package dev.updater.matcher.classifier

data class ClassifierResult<T>(val classifier: Classifier<T>, val score: Double)