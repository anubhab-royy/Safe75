# ---------------------------------------------------------------------------
# Safe75 - ProGuard / R8 keep rules
# ---------------------------------------------------------------------------

# --- Kotlin Serialization -------------------------------------------------
# Keep generated serializer classes and the serializers companion metadata.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.attendance.tracker.**$$serializer { *; }
-keepclassmembers class com.attendance.tracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.attendance.tracker.** { kotlinx.serialization.KSerializer serializer(...); }

# --- Room ----------------------------------------------------------------
# Room generates its own implementations and R8 keep rules via annotations,
# but keep entities and DAO method signatures that could be referenced by name.
-keep class com.attendance.tracker.data.local.database.entity.** { *; }
-keep class com.attendance.tracker.data.local.database.dao.** { *; }
-keep class com.attendance.tracker.data.local.database.AppDatabase { *; }

# --- Hilt / Dagger -------------------------------------------------------
# Dagger + Hilt ship their own consumer rules; this is a defensive fallback.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.assisted.AssistedInject class * { *; }
-keepclassmembers class * {
    @dagger.assisted.AssistedInject <init>(...);
    @dagger.assisted.Assisted <fields>;
}

# --- WorkManager ----------------------------------------------------------
# Worker subclasses must retain their no-arg / assisted constructors.
-keep class com.attendance.tracker.core.worker.** { *; }
-keepclassmembers class * extends androidx.work.Worker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- ML Kit ---------------------------------------------------------------
# REQUIRED: R8 obfuscates ML Kit's internal runtime classes, which makes
# TextRecognition.process() throw a NullPointerException ("getClass() on a null
# object reference") in minified release builds (verified via Phase B2 device
# logs + release mapping: 322+ renamed classes across mlkit_vision_common and
# mlkit_vision_text_common). Keep ML Kit's public API and its internal runtime
# packages referenced during text recognition.
-keep class com.google.mlkit.common.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.android.gms.internal.mlkit_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_bundled_common.** { *; }

# App-side OCR entry points (ML Kit callers).
-keep class com.attendance.tracker.feature.ocr.** { *; }

# --- OpenCV ---------------------------------------------------------------
# REQUIRED: the OpenCV 4.9.0 AAR ships NO consumer ProGuard rules (verified by
# inspecting the artifact), yet its Java wrapper declares `native` methods
# (e.g. org.opencv.core.Mat.n_Mat()) that are resolved at runtime by the JNI
# name-mangling convention against symbols exported from libopencv_java4.so
# (e.g. Java_org_opencv_core_Mat_n_1Mat). Obfuscating org.opencv.* renames the
# class and its native method declarations, breaking symbol resolution and
# causing UnsatisfiedLinkError on the first OCR operation in minified builds.
# This rule is scoped to the third-party org.opencv package only (fully owned
# by the library); no application classes are affected.
-keep class org.opencv.** { *; }

# --- Glance App Widget ----------------------------------------------------
# Glance widget classes must be referenced from the manifest by name.
-keep class com.attendance.tracker.feature.widget.** { *; }
-keep class com.attendance.tracker.core.widget.** { *; }

# --- Broadcast Receivers --------------------------------------------------
-keep class com.attendance.tracker.core.notification.AttendanceActionReceiver { *; }

# --- Miscellaneous safety -------------------------------------------------
-keepclassmembers enum * { *; }
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
