package dev.updater.matcher.classifier

import dev.updater.matcher.asm.MethodInstance
import org.objectweb.asm.Opcodes.*

object MethodClassifier : AbstractClassifier<MethodInstance>() {

    override fun init() {
        addClassifier(methodTypeCheck, weight = 10)
        addClassifier(accessFlags, weight = 4)
        addClassifier(argTypes, weight = 10)
        addClassifier(retType, weight = 5)
        addClassifier(stringConstants, weight = 5)
        addClassifier(numberConstants, weight = 5)
        addClassifier(parentMethod, weight = 10)
        addClassifier(childMethods, weight = 3)
        addClassifier(classRefs, weight = 3)
        addClassifier(inRefs, weight = 6)
        addClassifier(outRefs, weight = 6)
        addClassifier(fieldReadRefs, weight = 5)
        addClassifier(fieldWriteRefs, weight = 5)
        addClassifier(code, weight = 12)
    }

    fun rank(src: MethodInstance, dsts: List<MethodInstance>, maxMismatch: Double): List<RankResult<MethodInstance>> {
        return ClassifierUtil.rank(src, dsts, classifiers, maxMismatch, ClassifierUtil::isPotentiallyEqual)
    }

    private val methodTypeCheck = classifier("method type check") { a, b ->
        if(!checkAsmNodes(a, b)) return@classifier compareAsmNodes(a, b)

        val mask = ACC_STATIC or ACC_NATIVE or ACC_ABSTRACT
        val resultA = a.asmNode!!.access and mask
        val resultB = b.asmNode!!.access and mask

        return@classifier 1.0 - Integer.bitCount(resultA.xor(resultB)) / 3.0
    }

    private val accessFlags = classifier("access flags") { a, b ->
        if(!checkAsmNodes(a, b)) return@classifier compareAsmNodes(a, b)

        val mask = (ACC_PUBLIC or ACC_PROTECTED or ACC_PRIVATE) or ACC_FINAL or ACC_SYNCHRONIZED or ACC_BRIDGE or ACC_VARARGS or ACC_STRICT or ACC_SYNTHETIC
        val resultA = a.asmNode!!.access and mask
        val resultB = b.asmNode!!.access and mask

        return@classifier 1.0 - Integer.bitCount(resultA.xor(resultB)) / 8.0
    }

    private val argTypes = classifier("arg types") { a, b ->
        return@classifier ClassifierUtil.compareClassLists(a.args, b.args)
    }

    private val retType = classifier("return type") { a, b ->
        return@classifier if(ClassifierUtil.isPotentiallyEqual(a.retType, b.retType)) 1.0 else 0.0
    }

    private val stringConstants = classifier("string constants") { a, b ->
        if(!checkAsmNodes(a, b)) return@classifier compareAsmNodes(a, b)

        val stringsA = hashSetOf<String>()
        val stringsB = hashSetOf<String>()

        ClassifierUtil.extractStrings(a.asmNode!!.instructions, stringsA)
        ClassifierUtil.extractStrings(b.asmNode!!.instructions, stringsB)

        return@classifier ClassifierUtil.compareSets(stringsA, stringsB, false)
    }

    private val numberConstants = classifier("number constants") { a, b ->
        if(!checkAsmNodes(a, b)) return@classifier compareAsmNodes(a, b)

        val intsA = hashSetOf<Int>()
        val intsB = hashSetOf<Int>()
        val longsA = hashSetOf<Long>()
        val longsB = hashSetOf<Long>()
        val floatsA = hashSetOf<Float>()
        val floatsB = hashSetOf<Float>()
        val doublesA = hashSetOf<Double>()
        val doublesB = hashSetOf<Double>()

        ClassifierUtil.extractNumbers(a.asmNode!!, intsA, longsA, floatsA, doublesA)
        ClassifierUtil.extractNumbers(b.asmNode!!, intsB, longsB, floatsB, doublesB)

        return@classifier (ClassifierUtil.compareSets(intsA, intsB, false)
            + ClassifierUtil.compareSets(longsA, longsB, false)
            + ClassifierUtil.compareSets(floatsA, floatsB, false)
            + ClassifierUtil.compareSets(doublesA, doublesB, false)) / 4.0
    }

    private val classRefs = classifier("class refs") { a, b ->
        return@classifier ClassifierUtil.compareClassSets(a.classRefs, b.classRefs, true)
    }

    private val parentMethod = classifier("parent method") { a, b ->
        val parentA = a.parent
        val parentB = b.parent

        if((parentA == null) != (parentB == null)) return@classifier 0.0

        if(parentA == null) {
            return@classifier 1.0
        } else {
             return@classifier if(ClassifierUtil.isPotentiallyEqual(parentA, parentB!!)) 1.0 else 0.0
        }
    }

    private val childMethods = classifier("child methods") { a, b ->
        return@classifier ClassifierUtil.compareMethodSets(a.children, b.children, true)
    }

    private val outRefs = classifier("out refs") { a, b ->
        return@classifier ClassifierUtil.compareMethodSets(a.refsOut, b.refsOut, true)
    }

    private val inRefs = classifier("in refs") { a, b ->
        return@classifier ClassifierUtil.compareMethodSets(a.refsIn, b.refsIn, true)
    }

    private val fieldReadRefs = classifier("field read refs") { a, b ->
        return@classifier ClassifierUtil.compareFieldSets(a.fieldReadRefs, b.fieldReadRefs, true)
    }

    private val fieldWriteRefs = classifier("field write refs") { a, b ->
        return@classifier ClassifierUtil.compareFieldSets(a.fieldWriteRefs, b.fieldWriteRefs, true)
    }

    private val code = classifier("code") { a, b ->
        if(!checkAsmNodes(a, b)) return@classifier compareAsmNodes(a, b)
        return@classifier ClassifierUtil.compareInsns(a, b)
    }

    private fun checkAsmNodes(a: MethodInstance, b: MethodInstance): Boolean {
        return a.asmNode != null && b.asmNode != null
    }

    private fun compareAsmNodes(a: MethodInstance, b: MethodInstance): Double {
        return if(a.asmNode == null && b.asmNode == null) 1.0 else 0.0
    }
}