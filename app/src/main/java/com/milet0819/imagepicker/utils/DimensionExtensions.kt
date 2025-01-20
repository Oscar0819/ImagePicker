package com.milet0819.imagepicker.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue

// dp를 px로 변환하는 확장 함수
fun Float.toPx(context: Context): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        context.resources.displayMetrics
    )
}

// px를 dp로 변환하는 확장 함수
fun Float.toDp(context: Context): Float {
    val density = context.resources.displayMetrics.density
    return this / density
}

// Int 타입에도 적용할 수 있도록 추가 확장 함수
fun Int.toPx(context: Context): Int {
    return this.toFloat().toPx(context).toInt()
}

fun Int.toDp(context: Context): Int {
    return this.toFloat().toDp(context).toInt()
}