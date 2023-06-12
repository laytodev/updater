package dev.updater.matcher.classifier

import dev.updater.matcher.asm.ClassInstance
import dev.updater.matcher.asm.FieldInstance
import dev.updater.matcher.asm.MethodInstance
import org.objectweb.asm.Opcodes.*
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.max

object ClassClassifier : AbstractClassifier<ClassInstance>() {

    override fun init() {
        addClassifier(classTypeCheck, weight = 20)
        addClassifier(hierarchyDepth, weight = 1)
        addClassifier(hierarchySiblings, weight = 2)
        addClassifier(superClass, weight = 4)
        addClassifier(childClasses, weight = 3)
        addClassifier(interfaceClasses, weight = 3)
        addClassifier(implementerClasses, weight = 2)
        addClassifier(outerClass, weight = 6)
        addClassifier(innerClasses, weight = 5)
        addClassifier(methodCount, weight = 3)
        addClassifier(fieldCount, weight = 3)
        addClassifier(similarMemberMethods, weight = 10)
        addClassifier(outRefs, weight = 6)
        addClassifier(inRefs, weight = 6)
        addClassifier(stringConstants, weight = 8)
        addClassifier(numberConstants, weight = 6)
        addClassifier(methodOutRefs, weight = 5)
        addClassifier(methodInRefs, weight = 6)
        addClassifier(fieldReadRefs, weight = 5)
        addClassifier(fieldWriteRefs, weight = 5)
    }

    fun rank(src: ClassInstance, dsts: List<ClassInstance>, maxMismatch: Double): List<RankResult<ClassInstance>> {
        return ClassifierUtil.rank(src, dsts, classifiers, maxMismatch) { a, b -> ClassifierUtil.isPotentiallyEqual(a, b) }
    }

    private val classTypeCheck = classifier("class type check") { a, b ->
        val mask = (ACC_ENUM or ACC_INTERFACE or ACC_ANNOTATION) or ACC_ABSTRACT
        val resultA = a.asmNode!!.access and mask
        val resultB = b.asmNode!!.access and mask
        return@classifier 1.0 - Integer.bitCount(resultA.xor(resultB)) / 3.0
    }

    private val hierarchyDepth = classifier("hierarchy depth") { a, b ->
        var countA = 0
        var countB = 0

        var clsA = a
        var clsB = b

        while(clsA.superClass != null) {
            clsA = clsA.superClass!!
            countA++
        }

        while(clsB.superClass != null) {
            clsB = clsB.superClass!!
            countB++
        }

        return@classifier ClassifierUtil.compareCounts(countA, countB)
    }

    private val hierarchySiblings = classifier("hierarchy siblings") { a, b ->
        return@classifier ClassifierUtil.compareCounts(a.superClass!!.children.size, b.superClass!!.children.size)
    }

    private val superClass = classifier("super class") { a, b ->
        if(a.superClass == null && b.superClass == null) return@classifier 1.0
        if(a.superClass == null || b.superClass == null) return@classifier 0.0
        return@classifier if(ClassifierUtil.isPotentiallyEqual(a.superClass!!, b.superClass!!)) 1.0 else 0.0
    }

    private val childClasses = classifier("child classes") { a, b ->
        return@classifier ClassifierUtil.compareClassSets(a.children, b.children, true)
    }

    private val interfaceClasses = classifier("interface classes") { a, b ->
        return@classifier ClassifierUtil.compareClassSets(a.interfaces, b.interfaces, true)
    }

    private val implementerClasses = classifier("implementer classes") { a, b ->
        return@classifier ClassifierUtil.compareClassSets(a.implementers, b.implementers, true)
    }

    private val outerClass = classifier("outer class") { a, b ->
        val outerA = a.outerClass
        val outerB = b.outerClass

        if(outerA == null && outerB == null) return@classifier 1.0
        if(outerA == null || outerB == null) return@classifier 0.0

        return@classifier if(ClassifierUtil.isPotentiallyEqual(outerA, outerB)) 1.0 else 0.0
    }

    private val innerClasses = classifier("inner classes") { a, b ->
        val innerA = a.innerClasses
        val innerB = b.innerClasses

        if(innerA.isEmpty() && innerB.isEmpty()) return@classifier 1.0
        if(innerA.isEmpty() || innerB.isEmpty()) return@classifier 0.0

        return@classifier ClassifierUtil.compareClassSets(innerA, innerB, true)
    }

    private val methodCount = classifier("member method count") { a, b ->
        return@classifier ClassifierUtil.compareCounts(a.methods.filter { !it.isStatic() }.size, b.methods.filter { !it.isStatic() }.size)
    }

    private val fieldCount = classifier("member field count") { a, b ->
        return@classifier ClassifierUtil.compareCounts(a.fields.filter { !it.isStatic() }.size, b.fields.filter { !it.isStatic() }.size)
    }

    private val similarMemberMethods = classifier("similar member methods") { a, b ->
        if(a.memberMethods.isEmpty() && b.memberMethods.isEmpty()) return@classifier 1.0
        if(a.memberMethods.isEmpty() || b.memberMethods.isEmpty()) return@classifier 0-.0

        val methodsB = Collections.newSetFromMap<MethodInstance>(IdentityHashMap()).also { it.addAll(b.memberMethods) }
        var totalScore = 0.0
        var bestScore = 0.0
        var bestMatch: MethodInstance? = null

        a.memberMethods.forEach { methodA ->
            methodBLoop@ for(methodB in methodsB) {
                if(!ClassifierUtil.isPotentiallyEqual(methodA, methodB)) continue
                if(!ClassifierUtil.isPotentiallyEqual(methodA.retType, methodB.retType)) continue

                val argsA = methodA.args
                val argsB = methodB.args
                if(argsA.size != argsB.size) continue

                for(i in argsA.indices) {
                    val argA = argsA[i]
                    val argB = argsB[i]

                    if(!ClassifierUtil.isPotentiallyEqual(argA, argB)) {
                        continue@methodBLoop
                    }
                }

                val asmNodeA = methodA.asmNode
                val asmNodeB = methodB.asmNode

                var score: Double
                if(asmNodeA == null || asmNodeB == null) {
                    score = if(asmNodeA == null && asmNodeB == null) 1.0 else 0.0
                } else {
                    score = ClassifierUtil.compareCounts(asmNodeA.instructions.size(), asmNodeB.instructions.size())
                }

                if(score > bestScore) {
                    bestScore = score
                    bestMatch = methodB
                }
            }

            if(bestMatch != null) {
                totalScore += bestScore
                methodsB.remove(bestMatch)
            }
        }

        return@classifier totalScore / max(a.memberMethods.size, b.memberMethods.size).toDouble()
    }

    private val outRefs = classifier("out references") { a, b ->
        val refsA = a.outRefs
        val refsB = b.outRefs
        return@classifier ClassifierUtil.compareClassSets(refsA, refsB, false)
    }

    private val inRefs = classifier("in references") { a, b ->
        val refsA = a.inRefs
        val refsB = b.inRefs
        return@classifier ClassifierUtil.compareClassSets(refsA, refsB, false)
    }

    private val ClassInstance.outRefs: HashSet<ClassInstance> get() {
        val ret = hashSetOf<ClassInstance>()
        memberMethods.forEach { ret.addAll(it.classRefs) }
        memberFields.forEach { ret.add(it.type) }
        return ret
    }

    private val ClassInstance.inRefs: HashSet<ClassInstance> get() {
        val ret = hashSetOf<ClassInstance>()
        methodTypeRefs.forEach { ret.add(it.cls) }
        fieldTypeRefs.forEach { ret.add(it.cls) }
        return ret
    }

    private val stringConstants = classifier("string constants") { a, b ->
        return@classifier ClassifierUtil.compareSets(a.strings, b.strings, true)
    }

    private val numberConstants = classifier("number constants") { a, b ->
        val intsA = hashSetOf<Int>()
        val intsB = hashSetOf<Int>()
        val longsA = hashSetOf<Long>()
        val longsB = hashSetOf<Long>()
        val floatsA = hashSetOf<Float>()
        val floatsB = hashSetOf<Float>()
        val doublesA = hashSetOf<Double>()
        val doublesB = hashSetOf<Double>()

        extractNumbers(a, intsA, longsA, floatsA, doublesA)
        extractNumbers(b, intsB, longsB, floatsB, doublesB)

        return@classifier (ClassifierUtil.compareSets(intsA, intsB, false) +
            ClassifierUtil.compareSets(longsA, longsB, false) +
            ClassifierUtil.compareSets(floatsA, floatsB, false) +
            ClassifierUtil.compareSets(doublesA, doublesB, false)) / 4.0
    }

    private fun extractNumbers(cls: ClassInstance, ints: HashSet<Int>, longs: HashSet<Long>, floats: HashSet<Float>, doubles: HashSet<Double>) {
        cls.methods.forEach { method ->
            if(method.asmNode == null) return@forEach
            ClassifierUtil.extractNumbers(method.asmNode, ints, longs, floats, doubles)
        }

        cls.fields.forEach { field ->
            if(field.asmNode == null) return@forEach
            ClassifierUtil.handleNumberValue(field.asmNode.value, ints, longs, floats, doubles)
        }
    }

    private val methodOutRefs = classifier("method out references") { a, b ->
        val refsA = a.methodOutRefs
        val refsB = b.methodOutRefs
        return@classifier ClassifierUtil.compareMethodSets(refsA, refsB, false)
    }

    private val methodInRefs = classifier("method in references") { a, b ->
        val refsA = a.methodInRefs
        val refsB = b.methodInRefs
        return@classifier ClassifierUtil.compareMethodSets(refsA, refsB, false)
    }

    private val ClassInstance.methodOutRefs: HashSet<MethodInstance> get() {
        val ret = hashSetOf<MethodInstance>()
        memberMethods.forEach { method ->
            ret.addAll(method.refsOut)
        }
        return ret
    }

    private val ClassInstance.methodInRefs: HashSet<MethodInstance> get() {
        val ret = hashSetOf<MethodInstance>()
        memberMethods.forEach { method ->
            ret.addAll(method.refsIn)
        }
        return ret
    }

    private val fieldReadRefs = classifier("field read references") { a, b ->
        val refsA = a.fieldReadRefs
        val refsB = b.fieldReadRefs
        return@classifier ClassifierUtil.compareFieldSets(refsA, refsB, false)
    }

    private val fieldWriteRefs = classifier("field write references") { a, b ->
        val refsA = a.fieldWriteRefs
        val refsB = b.fieldWriteRefs
        return@classifier ClassifierUtil.compareFieldSets(refsA, refsB, false)
    }

    private val ClassInstance.fieldReadRefs: HashSet<FieldInstance> get() {
        val ret = hashSetOf<FieldInstance>()
        memberMethods.forEach { method ->
            ret.addAll(method.fieldReadRefs)
        }
        return ret
    }

    private val ClassInstance.fieldWriteRefs: HashSet<FieldInstance> get() {
        val ret = hashSetOf<FieldInstance>()
        memberMethods.forEach { method ->
            ret.addAll(method.fieldWriteRefs)
        }
        return ret
    }
}