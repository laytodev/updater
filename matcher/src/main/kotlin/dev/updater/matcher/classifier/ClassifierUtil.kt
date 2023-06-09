package dev.updater.matcher.classifier

import dev.updater.matcher.asm.ClassInstance
import dev.updater.matcher.asm.FieldInstance
import dev.updater.matcher.asm.MemberInstance
import dev.updater.matcher.asm.MethodInstance
import kotlin.math.abs
import kotlin.math.max

@Suppress("RedundantIf")
object ClassifierUtil {

    fun isPotentiallyEqual(a: ClassInstance, b: ClassInstance): Boolean {
        if(a == b) return true
        if(a.hasMatch()) return a.match == b
        if(b.hasMatch()) return b.match == a
        if(a.isArray() != b.isArray()) return false
        if(a.isArray() && !isPotentiallyEqual(a.elementClass!!, b.elementClass!!)) return false
        return true
    }

    fun isPotentiallyEqual(a: MethodInstance, b: MethodInstance): Boolean {
        if(a == b) return true
        if(a.hasMatch()) return a.match == b
        if(b.hasMatch()) return b.match == a
        if(!a.isStatic() && !b.isStatic()) {
            if(!isPotentiallyEqual(a.cls, b.cls)) return false
        }
        if((a.id.startsWith("<") || b.id.startsWith("<")) && a.name != b.name) return false
        return true
    }

    fun isPotentiallyEqual(a: FieldInstance, b: FieldInstance): Boolean {
        if(a == b) return true
        if(a.hasMatch()) return a.match == b
        if(b.hasMatch()) return b.match == a
        if(!a.isStatic() && !b.isStatic()) {
            if(!isPotentiallyEqual(a.cls, b.cls)) return false
        }
        return true
    }

    fun isPotentiallyEqual(a: MemberInstance<*>, b: MemberInstance<*>) {
        when(a) {
            is MethodInstance -> isPotentiallyEqual(a, b as MethodInstance)
            is FieldInstance -> isPotentiallyEqual(a, b as FieldInstance)
        }
    }

    fun isPotentiallyEqual(a: ClassInstance?, b: ClassInstance?): Boolean {
        if(a == null && b == null) return true
        if(a == null || b == null) return false
        return isPotentiallyEqual(a, b)
    }

    fun isPotentiallyEqual(a: MethodInstance?, b: MethodInstance?): Boolean {
        if(a == null && b == null) return true
        if(a == null || b == null) return false
        return isPotentiallyEqual(a, b)
    }

    fun isPotentiallyEqual(a: FieldInstance?, b: FieldInstance?): Boolean {
        if(a == null && b == null) return true
        if(a == null || b == null) return false
        return isPotentiallyEqual(a, b)
    }

    fun compareCounts(countA: Int, countB: Int): Double {
        val delta = abs(countA - countB)
        return if (delta == 0) 1.0 else 1.0 - delta.toDouble() / max(countA, countB)
    }


}