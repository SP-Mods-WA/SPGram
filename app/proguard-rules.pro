
# =========================================================
# TDLib
# =========================================================
-keep class org.drinkless.tdlib.** { *; }
-dontwarn org.drinkless.tdlib.**

# =========================================================
# Koin
# =========================================================
-keep class org.koin.** { *; }
-keepnames class org.koin.** { *; }
-dontwarn org.koin.**

# =========================================================
# Decompose
# =========================================================
-keep class com.arkivanov.decompose.** { *; }
-dontwarn com.arkivanov.decompose.**
-keep class com.arkivanov.essenty.** { *; }
-dontwarn com.arkivanov.essenty.**
-keep class com.arkivanov.mvikotlin.** { *; }
-dontwarn com.arkivanov.mvikotlin.**

# =========================================================
# Kotlinx Serialization
# =========================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.spmods.spgram.**$$serializer { *; }
-keepclassmembers class com.spmods.spgram.** {
    *** Companion;
}
-keepclasseswithmembers class com.spmods.spgram.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# =========================================================
# Kotlin Parcelize
# =========================================================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# =========================================================
# App classes — keep all domain/data/presentation
# =========================================================
-keep class com.spmods.spgram.** { *; }

# =========================================================
# Coil
# =========================================================
-dontwarn coil.**
-keep class coil.** { *; }

# =========================================================
# MapLibre
# =========================================================
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**

# =========================================================
# Firebase / GMS
# =========================================================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# =========================================================
# OkHttp
# =========================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# =========================================================
# Room
# =========================================================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# =========================================================
# Coroutines
# =========================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# =========================================================
# Crash handler — stack trace readable
# =========================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# =========================================================
# UnifiedPush
# =========================================================
-keep class org.unifiedpush.** { *; }
-dontwarn org.unifiedpush.**
