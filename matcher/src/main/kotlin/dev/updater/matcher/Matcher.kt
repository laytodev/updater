package dev.updater.matcher

import dev.updater.matcher.asm.*
import dev.updater.matcher.classifier.ClassClassifier
import dev.updater.matcher.classifier.RankResult
import dev.updater.matcher.gui.MatcherApp
import org.tinylog.kotlin.Logger
import tornadofx.launch
import java.io.File
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.streams.toList

object Matcher {

    lateinit var env: ClassEnvironment private set

    @JvmStatic
    fun main(args: Array<String>) {
        if(args.size < 2) error("Usage: <named jar> <deob jar>")

        val jarA = File(args[0])
        val jarB = File(args[1])

        init(jarA, jarB)
        autoMatchAll()

        launchGui()
    }

    fun init(jarA: File, jarB: File) {
        Logger.info("Initializing.")

        env = ClassEnvironment(jarA, jarB)
        env.init()

        //upddateUnmatchables()
        matchUnobfuscated()

        ClassClassifier.init()
        MethodClassifier.init()
    }

    fun launchGui() {
        Logger.info("Launching matcher GUI.")
        launch<MatcherApp>()
    }

    private fun upddateUnmatchables() {
        env.groupA.classes.filter { it.name.startsWith("org/json") || it.name.startsWith("org/bouncycastle") }.forEach { cls ->
            cls.isMatchable = false
            cls.methods.forEach { it.isMatchable = false }
            cls.fields.forEach { it.isMatchable = false }
        }

        env.groupB.classes.filter { it.name.startsWith("org/json") || it.name.startsWith("org/bouncycastle") }.forEach { cls ->
            cls.isMatchable = false
            cls.methods.forEach { it.isMatchable = false }
            cls.fields.forEach { it.isMatchable = false }
        }
    }

    private fun matchUnobfuscated() {
        env.groupA.classes.filter { it.isMatchable }.forEach { cls ->
            if(cls.isNameObfuscated) return@forEach
            val match = env.groupB.getClass(cls.name)
            if(match != null && !match.isNameObfuscated) {
                match(cls, match)
            }
        }
    }

    fun match(a: ClassInstance, b: ClassInstance) {
        if(a.match == b) return
        if(a.hasMatch()) {
            a.match!!.match = null
            unmatchMembers(a)
        }
        if(b.hasMatch()) {
            b.match!!.match = null
            unmatchMembers(b)
        }
        a.match = b
        b.match = a

        if(a.isArray()) {
            val elemA = a.elementClass!!
            if(!elemA.hasMatch()) match(elemA, b.elementClass!!)
        } else {
            a.arrays.forEach { arrayA ->
                val dims = arrayA.dims
                for(arrayB in b.arrays) {
                    if(arrayB.hasMatch() || arrayB.dims != dims) continue
                    match(arrayA, arrayB)
                    break
                }
            }
        }

        a.memberMethods.forEach { src ->
            if(!src.isNameObfuscated) {
                var dst = b.getMethod(src.name, src.desc)
                if (dst != null || (b.getMethod(src.name, src.desc)
                        .also { dst = it } != null) && !dst!!.isNameObfuscated
                ) {
                    match(src, dst!!)
                }
            }
        }

        a.memberFields.forEach { src ->
            if(!src.isNameObfuscated) {
                var dst = b.getField(src.name, src.desc)
                if(dst != null || (b.getField(src.name, src.desc).also { dst = it } != null) && !dst!!.isNameObfuscated) {
                    match(src, dst!!)
                }
            }
        }

        Logger.info("Matched class $a -> $b")
    }

    fun match(a: MethodInstance, b: MethodInstance) {
        if(!a.isStatic() && !b.isStatic()) {
            if(a.cls.match != b.cls) throw IllegalArgumentException("Methods are not static and dont belong to the same class.")
        }
        if(a.match == b) return
        if(a.hasMatch()) a.match!!.match = null
        if(b.hasMatch()) b.match!!.match = null
        a.match = b
        b.match = a
        Logger.info("Matched ${if(a.isStatic()) "static" else "member"} method $a -> $b")
    }

    fun match(a: FieldInstance, b: FieldInstance) {
        if(!a.isStatic() && !b.isStatic()) {
            if(a.cls.match != b.cls) throw IllegalArgumentException("Fields are not static and dont belong to the same class.")
        }
        if(a.match == b) return
        if(a.hasMatch()) a.match!!.match = null
        if(b.hasMatch()) b.match!!.match = null
        a.match = b
        b.match = a
        Logger.info("Matched ${if(a.isStatic()) "static" else "member"} field $a -> $b")
    }

    fun unmatchMembers(cls: ClassInstance) {
        cls.methods.filter { !it.isStatic() }.forEach { method ->
            if(method.hasMatch()) {
                method.match!!.match = null
                method.match = null
            }
        }
        cls.fields.filter { !it.isStatic() }.forEach { field ->
            if(field.hasMatch()) {
                field.match!!.match = null
                field.match = null
            }
        }
    }

    fun autoMatchAll() {
        Logger.info("Matching...")
        if(autoMatchClasses(absClassAutoMatchThreshold, relClassAutoMatchThreshold) { }) {
            autoMatchClasses(absClassAutoMatchThreshold, relClassAutoMatchThreshold) { }
        }

        var matchedAny: Boolean
        do {
            matchedAny = autoMatchMemberMethods(absMethodAutoMatchThreshold, relMethodAutoMatchThreshold) {}
            matchedAny = matchedAny or autoMatchStaticMethods(absMethodAutoMatchThreshold, relMethodAutoMatchThreshold) {}
            matchedAny = matchedAny or autoMatchClasses(absClassAutoMatchThreshold, relClassAutoMatchThreshold) {}
        } while(matchedAny)
    }

    fun autoMatchClasses(normThreshold: Double, relThreshold: Double, progressCallback: (Double) -> Unit): Boolean {
        val absThreshold = normThreshold * ClassClassifier.maxScore

        val classes = env.groupA.classes.filter { it.isMatchable && !it.hasMatch() }
        val cmpClasses = env.groupB.classes.filter { it.isMatchable && !it.hasMatch() }

        val maxScore = ClassClassifier.maxScore
        val maxMismatch = maxScore - getRawScore(absThreshold * (1.0 - relThreshold), maxScore)
        val matches = ConcurrentHashMap<ClassInstance, ClassInstance>(classes.size)

        runInParallel(classes, { cls ->
            val ranking = ClassClassifier.rank(cls, cmpClasses, maxMismatch)

            if(checkRank(ranking, absThreshold, relThreshold, maxScore)) {
                val match = ranking[0].subject
                matches[cls] = match
            }
        }, progressCallback)

        sanitizeMatches(matches)

        matches.entries.forEach { entry ->
            match(entry.key, entry.value)
        }

        Logger.info("Auto-Matched ${matches.size} classes. (${classes.size - matches.size} unmatched, ${env.groupA.classes.size} total)")

        return matches.isNotEmpty()
    }

    fun autoMatchMemberMethods(normThreshold: Double, relThreshold: Double, progressCallback: (Double) -> Unit): Boolean {
        val absThreshold = normThreshold * MethodClassifier.maxScore

        val totalMethods = AtomicInteger()
        val totalMatched = AtomicInteger()
        val totalUnmatched = AtomicInteger()
        env.groupA.classes.filter { it.hasMatch() }.forEach { cls ->
            val methods = cls.memberMethods.filter { !it.hasMatch() }
            val cmpMethods = cls.match!!.memberMethods.filter { !it.hasMatch() }

            val maxScore = MethodClassifier.maxScore
            val maxMismatch = maxScore - getRawScore(absThreshold * (1.0 - relThreshold), maxScore)
            val matches = ConcurrentHashMap<MethodInstance, MethodInstance>()

            runInParallel(methods, { method ->
                val ranking = MethodClassifier.rank(method, cmpMethods, maxMismatch)

                if(checkRank(ranking, absThreshold, relThreshold, maxScore)) {
                    val match = ranking[0].subject
                    matches[method] = match
                }
            }, progressCallback)

            sanitizeMatches(matches)

            matches.entries.forEach { entry ->
                match(entry.key, entry.value)
            }

            totalMethods.getAndAdd(methods.size)
            totalMatched.getAndAdd(matches.size)
            totalUnmatched.getAndAdd(methods.size - matches.size)
        }

        Logger.info("Auto-Matched ${totalMatched.get()} member methods. (${totalUnmatched.get()} unmatched, ${totalMethods.get()} total)")

        return totalMatched.get() > 0
    }

    fun autoMatchStaticMethods(normThreshold: Double, relThreshold: Double, progressCallback: (Double) -> Unit): Boolean {
        val absThreshold = normThreshold * MethodClassifier.maxScore

        val methods = env.groupA.staticMethods.filter { !it.hasMatch() }
        val cmpMethods = env.groupB.staticMethods.filter { !it.hasMatch() }

        val maxScore = MethodClassifier.maxScore
        val maxMismatch = maxScore - getRawScore(absThreshold * (1.0 - relThreshold), maxScore)
        val matches = ConcurrentHashMap<MethodInstance, MethodInstance>()

        runInParallel(methods, { method ->
            val ranking = MethodClassifier.rank(method, cmpMethods, maxMismatch)
            if(checkRank(ranking, absThreshold, relThreshold, maxScore)) {
                val match = ranking[0].subject
                matches[method] = match
            }
        }, progressCallback)

        sanitizeMatches(matches)

        matches.entries.forEach { entry ->
            match(entry.key, entry.value)
        }

        Logger.info("Auto-Matched ${matches.size} static methods. (${methods.size - matches.size} unmatched, ${methods.size} total)")

        return matches.isNotEmpty()
    }

    private fun <T> runInParallel(workSet: List<T>, worker: (T) -> Unit, progressCallback: (Double) -> Unit) {
        if(workSet.isEmpty()) return

        val itemsDone = AtomicInteger()
        val updateRate = max(1, workSet.size / 200)
        val futures = threadPool.invokeAll(workSet.stream().map { workItem: T ->
                Callable<Void> {
                    worker(workItem)
                    val cItemsDone = itemsDone.incrementAndGet()
                    if (cItemsDone % updateRate == 0) {
                        progressCallback(cItemsDone.toDouble() / workSet.size)
                    }
                    null
                }
            }.toList())
        futures.forEach { it.get() }
    }

    private fun checkRank(ranking: List<RankResult<*>>, absThreshold: Double, relThreshold: Double, maxScore: Double): Boolean {
        if(ranking.isEmpty()) return false

        val score = ranking[0].score
        if(score < absThreshold) return false

        return if(ranking.size == 1) true else {
            val nextScore = ranking[1].score
            nextScore < score * (1.0 - relThreshold)
        }
    }

    private fun <T> sanitizeMatches(matches: ConcurrentHashMap<T, T>) {
        val matched = Collections.newSetFromMap<T>(IdentityHashMap(matches.size))
        val conflictingMatches = Collections.newSetFromMap<T>(IdentityHashMap())

        matches.values.forEach { cls ->
            if(!matched.add(cls)) {
                conflictingMatches.add(cls)
            }
        }

        if(!conflictingMatches.isEmpty()) {
            matches.values.removeAll(conflictingMatches)
        }
    }

    private fun getScore(rawScore: Double, maxScore: Double): Double {
        val ret = rawScore / maxScore
        return ret * ret
    }

    private fun getRawScore(score: Double, maxScore: Double): Double {
        return sqrt(score) * maxScore
    }

    const val absClassAutoMatchThreshold = 0.8
    const val relClassAutoMatchThreshold = 0.08
    const val absMethodAutoMatchThreshold = 0.8
    const val relMethodAutoMatchThreshold = 0.08
    const val absFieldAutoMatchThreshold = 0.8
    const val relFieldAutoMatchThreshold = 0.08

    val threadPool = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors() - 1)
}