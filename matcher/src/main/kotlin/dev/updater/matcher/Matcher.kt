package dev.updater.matcher

import dev.updater.matcher.asm.ClassEnvironment
import dev.updater.matcher.asm.ClassInstance
import dev.updater.matcher.asm.FieldInstance
import dev.updater.matcher.asm.MethodInstance
import dev.updater.matcher.classifier.ClassClassifier
import dev.updater.matcher.classifier.RankResult
import dev.updater.matcher.gui.MatcherApp
import dev.updater.matcher.gui.ProgressUtil
import org.tinylog.kotlin.Logger
import tornadofx.launch
import java.io.File
import java.util.Collections
import java.util.HashMap
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

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

        matchUnobfuscated()

        ClassClassifier.init()
    }

    fun launchGui() {
        Logger.info("Launching matcher GUI.")
        launch<MatcherApp>()
    }

    private fun matchUnobfuscated() {
        env.groupA.classes.forEach { cls ->
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
        Logger.info("Matched method $a -> $b")
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
        Logger.info("Matched field $a -> $b")
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
        Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors()).execute {
            if(autoMatchClasses(absClassAutoMatchThreshold, relClassAutoMatchThreshold) { }) {
                autoMatchClasses(absClassAutoMatchThreshold, relClassAutoMatchThreshold) { }
            }

            var matchedAny: Boolean

            do {
                matchedAny = autoMatchClasses(absClassAutoMatchThreshold, relClassAutoMatchThreshold) { }
            } while(matchedAny)
        }
    }

    fun autoMatchClasses(normThreshold: Double, relThreshold: Double, progressCallback: (Double) -> Unit): Boolean {
        val absThreshold = normThreshold * ClassClassifier.maxScore

        val classes = env.groupA.classes.filter {!it.hasMatch() }
        val cmpClasses = env.groupB.classes.filter { !it.hasMatch() }

        val matches = ConcurrentHashMap<ClassInstance, ClassInstance>(classes.size)

        runInParallel(classes, { cls ->
            val ranking = ClassClassifier.rank(cls, cmpClasses)

            if(checkRank(ranking, absThreshold, relThreshold)) {
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

    private fun <T> runInParallel(workSet: List<T>, worker: (T) -> Unit, progressCallback: (Double) -> Unit) {
        if(workSet.isEmpty()) return

        val itemsDone = AtomicInteger()
        val updateRate = max(1, workSet.size / 200)

        workSet.parallelStream().forEach { workItem ->
            worker(workItem)

            val cItemsDone = itemsDone.incrementAndGet()

            if((cItemsDone % updateRate) == 0) {
                progressCallback(cItemsDone.toDouble() / workSet.size)
            }
        }
    }

    private fun checkRank(ranking: List<RankResult<*>>, absThreshold: Double, relThreshold: Double): Boolean {
        if(ranking.isEmpty()) return false
        if(ranking[0].score < absThreshold) return false

        return if(ranking.size == 1) {
            true
        } else {
            ranking[1].score < ranking[0].score * (1.0 - relThreshold)
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

    const val absClassAutoMatchThreshold = 0.8
    const val relClassAutoMatchThreshold = 0.08
    const val absMethodAutoMatchThreshold = 0.8
    const val relMethodAutoMatchThreshold = 0.08
    const val absFieldAutoMatchThreshold = 0.8
    const val relFieldAutoMatchThreshold = 0.08
}