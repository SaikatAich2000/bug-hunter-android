# Moshi
-keep,allowobfuscation,allowshrinking @interface com.squareup.moshi.JsonClass
-keep class **JsonAdapter { *; }
-keepclassmembers class kotlin.Metadata { *; }
-keepclasseswithmembers class * { @com.squareup.moshi.* <methods>; }
-keep class kotlin.reflect.jvm.internal.impl.** { *; }
-keepnames @com.squareup.moshi.JsonClass class *
-if @com.squareup.moshi.JsonClass class *
-keep class <1>JsonAdapter { <init>(...); <fields>; }

# Retrofit
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# App DTOs (Moshi-generated adapters reflect on these)
-keep class com.bughunter.core.network.dto.** { *; }
-keep class com.bughunter.core.domain.model.** { *; }

# WorkManager + Firebase: both load classes from string names at runtime
# (WorkManager from a serialized class-name in its DB, Firebase from the
# AndroidManifest service entry). R8 sees Worker subclasses only if it
# can prove the class reference; the manifest analyzer catches services
# but NOT the manifest-less Worker. Be explicit so push registration
# can never silently break in a shrunk build.
-keep class com.bughunter.core.push.** { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService { *; }

# Hilt-Work: generated factory registers workers via class name.
-keep class * extends androidx.hilt.work.HiltWorkerFactory { *; }
-keep @androidx.hilt.work.HiltWorker class * { *; }
