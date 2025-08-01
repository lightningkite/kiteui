package com.lightningkite.kiteui

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation


public fun KSAnnotated.annotation(name: String, packageName: String = "com.lightningkite.kiteui"): KSAnnotation? {
    return this.annotations.find {
        it.shortName.getShortName() == name &&
                it.annotationType.resolve().declaration.qualifiedName?.asString() == "$packageName.$name"
    }
}
public fun KSAnnotated.annotations(name: String, packageName: String = "com.lightningkite.kiteui"): List<KSAnnotation> {
    return this.annotations.filter {
        it.shortName.getShortName() == name &&
                it.annotationType.resolve().declaration.qualifiedName?.asString() == "$packageName.$name"
    }.toList()
}