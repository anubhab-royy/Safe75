# ---------------------------------------------------------------------------
# Attendance Tracker - ProGuard / R8 keep rules
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
# ML Kit bundles its own rules; keep parser/scheduler entry points.
-keep class com.attendance.tracker.feature.ocr.** { *; }

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
