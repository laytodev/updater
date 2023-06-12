package dev.updater.matcher.classifier

import dev.updater.matcher.Matcher
import dev.updater.matcher.asm.*
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.AbstractInsnNode.*
import org.tinylog.kotlin.Logger
import java.util.*
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.toList

@Suppress("RedundantIf")
object ClassifierUtil {

    fun isPotentiallyEqual(a: ClassInstance, b: ClassInstance): Boolean {
        if(a == b) return true
        if(a.hasMatch()) return a.match == b
        if(b.hasMatch()) return b.match == a
        if(!a.isMatchable || !b.isMatchable) return false
        if(a.isArray() != b.isArray()) return false
        if(a.isArray() && !isPotentiallyEqual(a.elementClass!!, b.elementClass!!)) return false
        if(!checkNameObfMatch(a, b)) return false
        return true
    }

    fun isPotentiallyEqual(a: MethodInstance, b: MethodInstance): Boolean {
        if(a == b) return true
        if(a.hasMatch()) return a.match == b
        if(b.hasMatch()) return b.match == a
        if(!a.isMatchable || !b.isMatchable) return false
        if(!a.isStatic() && !b.isStatic()) {
            if(!isPotentiallyEqual(a.cls, b.cls)) return false
        }
        if(!checkNameObfMatch(a, b)) return false
        if((a.id.startsWith("<") || b.id.startsWith("<")) && a.name != b.name) return false
        return true
    }

    fun isPotentiallyEqual(a: FieldInstance, b: FieldInstance): Boolean {
        if(a == b) return true
        if(a.hasMatch()) return a.match == b
        if(b.hasMatch()) return b.match == a
        if(!a.isMatchable || !b.isMatchable) return false
        if(!a.isStatic() && !b.isStatic()) {
            if(!isPotentiallyEqual(a.cls, b.cls)) return false
        }
        if(!checkNameObfMatch(a, b)) return false
        return true
    }

    fun <T : Matchable<T>> isPotentiallyEqual(a: T, b: T): Boolean {
        if(a::class != b::class) return false
        return when(a::class) {
            ClassInstance::class -> isPotentiallyEqual(a as ClassInstance, b as ClassInstance)
            MethodInstance::class -> isPotentiallyEqual(a as MethodInstance, b as MethodInstance)
            FieldInstance::class -> isPotentiallyEqual(a as FieldInstance, b as FieldInstance)
            else -> throw IllegalArgumentException("Unknown matchable type: ${a::class.simpleName}.")
        }
    }

    fun isPotentiallyEqualNullable(a: ClassInstance?, b: ClassInstance?): Boolean {
        if(a == null && b == null) return true
        if(a == null || b == null) return false
        return isPotentiallyEqual(a, b)
    }

    fun isPotentiallyEqualNullable(a: MethodInstance?, b: MethodInstance?): Boolean {
        if(a == null && b == null) return true
        if(a == null || b == null) return false
        return isPotentiallyEqual(a, b)
    }

    fun isPotentiallyEqualNullable(a: FieldInstance?, b: FieldInstance?): Boolean {
        if(a == null && b == null) return true
        if(a == null || b == null) return false
        return isPotentiallyEqual(a, b)
    }

    private fun checkNameObfMatch(a: Matchable<*>, b: Matchable<*>): Boolean {
        val nameObfA = a.isNameObfuscated
        val nameObfB = b.isNameObfuscated

        if(nameObfA && nameObfB) {
            return true
        } else if(nameObfA != nameObfB) {
            return true
        } else {
            return a.name == b.name
        }
    }

    fun compareCounts(countA: Int, countB: Int): Double {
        val delta = abs(countA - countB)
        return if (delta == 0) 1.0 else 1.0 - delta.toDouble() / max(countA, countB)
    }

    fun <T> compareSets(a: HashSet<T>, b: HashSet<T>, readOnly: Boolean): Double {
        val setA = a
        val setB = if(readOnly) hashSetOf<T>().also { it.addAll(b) } else b

        val oldSize = setB.size
        setB.removeAll(setA)

        val matched = oldSize - setB.size
        val total = setA.size - matched + oldSize

        return if(total == 0) 1.0 else matched.toDouble() / total.toDouble()
    }

    fun compareClassSets(a: HashSet<ClassInstance>, b: HashSet<ClassInstance>, readOnly: Boolean): Double {
        return compareMatchableSets(a, b, readOnly, ClassifierUtil::isPotentiallyEqual)
    }

    fun compareMethodSets(a: HashSet<MethodInstance>, b: HashSet<MethodInstance>, readOnly: Boolean): Double {
        return compareMatchableSets(a, b, readOnly, ClassifierUtil::isPotentiallyEqual)
    }

    fun compareFieldSets(a: HashSet<FieldInstance>, b: HashSet<FieldInstance>, readOnly: Boolean): Double {
        return compareMatchableSets(a, b, readOnly, ClassifierUtil::isPotentiallyEqual)
    }

    fun compareClassLists(a: MutableList<ClassInstance>, b: MutableList<ClassInstance>): Double {
        return compareLists(a, b, MutableList<ClassInstance>::get, MutableList<ClassInstance>::size) { a: ClassInstance, b: ClassInstance ->
            if (isPotentiallyEqual(a, b)) COMPARED_SIMILAR else COMPARED_DISTINCT
        }
    }

    private fun <T : Matchable<T>> compareMatchableSets(inSetA: HashSet<T>, inSetB: HashSet<T>, readOnly: Boolean, comparator: (a: T, b: T) -> Boolean): Double {
        if(inSetA.isEmpty() || inSetB.isEmpty()) {
            return if(inSetA.isEmpty() && inSetB.isEmpty()) 1.0 else 0.0
        }

        var setA = inSetA
        var setB = inSetB
        if(readOnly) {
            setA = hashSetOf<T>().also { it.addAll(inSetA) }
            setB = hashSetOf<T>().also { it.addAll(inSetB) }
        }

        val total = setA.size + setB.size
        var unmatched = 0

        val itA = setA.iterator()
        while(itA.hasNext()) {
            val a = itA.next()
            if(setB.remove(a)) {
                itA.remove()
            } else if(a.hasMatch()) {
                if(!setB.remove(a.match)) {
                    unmatched++
                }
                itA.remove()
            } // Remove unobfuscated names
        }

        val itB = setA.iterator()
        while(itB.hasNext()) {
            val a = itB.next()
            var found = false

            for(b in setB) {
                if(comparator(a, b)) {
                    found = true
                    break
                }
            }

            if(!found) {
                unmatched++
                itB.remove()
            }
        }

        setB.forEach { b ->
            var found = false
            for(a in setA) {
                if(comparator(a, b)) {
                    found = true
                    break
                }
            }

            if(!found) {
                unmatched++
            }
        }

        return (total - unmatched).toDouble() / total.toDouble()
    }

    private fun <T, U> compareLists(listA: T, listB: T, getElement: (lst: T, pos: Int) -> U, getListSize: (lst: T) -> Int, comparator: (a: U, b: U) -> Int): Double {
        val sizeA = getListSize(listA)
        val sizeB = getListSize(listB)

        if(sizeA == 0 && sizeB == 0) return 1.0
        if(sizeA == 0 || sizeB == 0) return 0.0

        if(sizeA == sizeB) {
            var match = true
            for(i in 0 until sizeA) {
                if(comparator(getElement(listA, i), getElement(listB, i)) != COMPARED_SIMILAR) {
                    match = false
                    break
                }
            }

            if(match) return 1.0
        }

        /*
         * Use the levenshtein distance alg
         */
        val v0 = IntArray(sizeB + 1)
        val v1 = IntArray(sizeB + 1)

        for(i in 1 until v0.size) {
            v0[i] = i * COMPARED_DISTINCT
        }

        for(i in 0 until sizeA) {
            v1[0] = (i + 1) * COMPARED_DISTINCT

            for(j in 0 until sizeB) {
                val cost = comparator(getElement(listA, i), getElement(listB, j))
                v1[j + 1] = min(min(v1[j] + COMPARED_DISTINCT, v0[j + 1] + COMPARED_DISTINCT), v0[j] + cost)
            }

            for(j in v0.indices) {
                v0[j] = v1[j]
            }
        }

        val distance = v1[sizeB]
        val upperBound = max(sizeA, sizeB) * COMPARED_DISTINCT
        assert(distance in 0..upperBound)

        return 1.0 - distance.toDouble() / upperBound.toDouble()
    }

    fun compareInsns(a: MethodInstance, b: MethodInstance): Double {
        if(a.asmNode == null || b.asmNode == null) return 1.0
        val insnsA = a.asmNode.instructions
        val insnsB = b.asmNode.instructions
        return compareLists(insnsA, insnsB, InsnList::get, InsnList::size) { insnA, insnB ->
            compareInsns(insnA, insnB, insnsA, insnsB, a, b) { lst, insn -> lst.indexOf(insn) }
        }
    }

    fun compareInsns(listA: List<AbstractInsnNode>, listB: List<AbstractInsnNode>): Double {
        return compareLists(listA, listB, List<AbstractInsnNode>::get, List<AbstractInsnNode>::size) { a, b ->
            compareInsns(a, b, listA, listB, null, null) { lst, insn -> lst.indexOf(insn) }
        }
    }

    private fun <T> compareInsns(insnA: AbstractInsnNode, insnB: AbstractInsnNode, listA: T, listB: T, mthA: MethodInstance?, mthB: MethodInstance?, getPosition: (lst: T, insn: AbstractInsnNode) -> Int): Int {
        if(insnA.opcode != insnB.opcode) return COMPARED_DISTINCT

        when(insnA.type) {
            INT_INSN -> {
                val a = insnA as IntInsnNode
                val b = insnB as IntInsnNode
                return if(a.operand == b.operand) COMPARED_SIMILAR else COMPARED_DISTINCT
            }

            TYPE_INSN -> {
                val a = insnA as TypeInsnNode
                val b = insnB as TypeInsnNode
                val clsA = Matcher.env.groupA.getClass(a.desc)
                val clsB = Matcher.env.groupB.getClass(b.desc)
                return if(isPotentiallyEqualNullable(clsA, clsB)) COMPARED_SIMILAR else COMPARED_DISTINCT
            }

            FIELD_INSN -> {
                val a = insnA as FieldInsnNode
                val b = insnB as FieldInsnNode
                val clsA = Matcher.env.groupA.getClass(a.owner)
                val clsB = Matcher.env.groupB.getClass(b.owner)

                if(clsA == null && clsB == null) return COMPARED_SIMILAR
                if(clsA == null || clsB == null) return COMPARED_DISTINCT

                val fieldA = clsA.resolveField(a.name, a.desc)
                val fieldB = clsB.resolveField(b.name, b.desc)

                return if(isPotentiallyEqualNullable(fieldA, fieldB)) COMPARED_SIMILAR else COMPARED_DISTINCT
            }

            METHOD_INSN -> {
                val a = insnA as MethodInsnNode
                val b = insnB as MethodInsnNode
                return if(compareMethods(a.owner, a.name, a.desc, b.owner, b.name, b.desc)) COMPARED_SIMILAR else COMPARED_DISTINCT
            }

            JUMP_INSN -> {
                val a = insnA as JumpInsnNode
                val b = insnB as JumpInsnNode
                val dirA = Integer.signum(getPosition(listA, a.label) - getPosition(listA, a))
                val dirB = Integer.signum(getPosition(listB, b.label) - getPosition(listB, b))
                return if(dirA == dirB) COMPARED_SIMILAR else COMPARED_DISTINCT
            }
        }
        return COMPARED_SIMILAR
    }

    private fun compareMethods(
        ownerA: String,
        nameA: String,
        descA: String,
        ownerB: String,
        nameB: String,
        descB: String
    ) : Boolean {
        val clsA = Matcher.env.groupA.getClass(ownerA)
        val clsB = Matcher.env.groupB.getClass(ownerB)

        if(clsA == null && clsB == null) return true
        if(clsA == null || clsB == null) return false

        return compareMethods(clsA, nameA, descA, clsB, nameB, descB)

        val v = ClassLoader.getSystemClassLoader()
    }

    private fun compareMethods(clsA: ClassInstance, nameA: String, descA: String, clsB: ClassInstance, nameB: String, descB: String): Boolean {
        val methodA = clsA.resolveMethod(nameA, descA)
        val methodB = clsB.resolveMethod(nameB, descB)

        if(methodA == null && methodB == null) return true
        if(methodA == null || methodB == null) return false

        return isPotentiallyEqual(methodA, methodB)
    }

    fun <T> rankParallel(src: T, dsts: List<T>, classifiers: Map<Classifier<T>, Double>, maxMismatch: Double, checkEquality: (a: T, b: T) -> Boolean): List<RankResult<T>> {
        return dsts.stream()
            .parallel()
            .map { dst: T -> rank<T>(src, dst, classifiers.keys.toList(), maxMismatch, checkEquality) }
            .filter { it != null }
            .sorted(Comparator.comparing(RankResult<T>::score).reversed())
            .toList()
            .filterNotNull()
    }

    fun <T> rank(src: T, dsts: List<T>, classifiers: Map<Classifier<T>, Double>, maxMismatch: Double, checkEquality: (a: T, b: T) -> Boolean): List<RankResult<T>> {
        val ret = mutableListOf<RankResult<T>>()

        for(dst in dsts) {
            val result = rank(src, dst, classifiers.keys.toList(), maxMismatch, checkEquality)
            if(result != null) ret.add(result)
        }
        return ret.sortedBy(RankResult<T>::score).asReversed()
    }

    private fun <T> rank(src: T, dst: T, classifiers: List<Classifier<T>>, maxMismatch: Double, checkEquality: (a: T, b: T) -> Boolean): RankResult<T>? {
        if(!checkEquality(src, dst)) return null

        var score = 0.0
        val results = mutableListOf<ClassifierResult<T>>()

        for(classifier in classifiers) {
            val cscore = classifier.getScore(src, dst)
            val weight = classifier.weight
            val weightedScore = cscore * weight
            score += weightedScore
            results.add(ClassifierResult(classifier, cscore))
        }
        return RankResult(dst, score, results)
    }

    fun extractStrings(insns: InsnList, out: HashSet<String>) {
        extractStrings(insns.iterator(), out)
    }

    fun extractStrings(insns: List<AbstractInsnNode>, out: HashSet<String>) {
        extractStrings(insns.iterator(), out)
    }

    private fun extractStrings(it: Iterator<AbstractInsnNode>, out: HashSet<String>) {
        while(it.hasNext()) {
            val insn = it.next()
            if(insn is LdcInsnNode) {
                if(insn.cst is String) {
                    out.add(insn.cst as String)
                }
            }
        }
    }

    fun extractNumbers(node: MethodNode, ints: HashSet<Int>, longs: HashSet<Long>, floats: HashSet<Float>, doubles: HashSet<Double>) {
        val it = node.instructions.iterator()
        while(it.hasNext()) {
            val insn = it.next()
            if(insn is LdcInsnNode) {
                handleNumberValue(insn.cst, ints, longs, floats, doubles)
            } else if(insn is IntInsnNode) {
                ints.add(insn.operand)
            }
        }
    }

    fun handleNumberValue(number: Any?, ints: HashSet<Int>, longs: HashSet<Long>, floats: HashSet<Float>, doubles: HashSet<Double>) {
        if(number == null) return
        if(number is Int) ints.add(number)
        else if(number is Long) longs.add(number)
        else if(number is Float) floats.add(number)
        else if(number is Double) doubles.add(number)
    }

    const val COMPARED_SIMILAR = 0
    const val COMPARED_POSSIBLE = 1
    const val COMPARED_DISTINCT = 2
}