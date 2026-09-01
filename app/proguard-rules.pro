# ============================================================
# Kotlin & Coroutines
# ============================================================
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod
-dontwarn kotlinx.coroutines.**

# ============================================================
# Hilt / Dagger
# ============================================================
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keepclassmembers class * { @dagger.hilt.android.lifecycle.HiltViewModel <init>(...); }

# ============================================================
# Room
# ============================================================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-keep class androidx.room.** { *; }

# ============================================================
# Coil
# ============================================================
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================================
# Compose
# ============================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ============================================================
# AndroidX Startup — InitializationProvider
# R8 strips the provider metadata if not kept, causing
# "PackageInfo is null" crashes at startup.
# ============================================================
-keep class androidx.startup.** { *; }
-keep class androidx.profileinstaller.** { *; }

# ============================================================
# Glance Widgets — keep widget receivers and metadata
# ============================================================
-keep class androidx.glance.** { *; }
-keep class androidx.work.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }

# Keep all classes referenced in AndroidManifest.xml
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Service
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Activity

# ============================================================
# Kotlin data classes used by Room/serialization
# ============================================================
-keepclassmembers class com.f1pulse.app.data.** {
    *;
}
-keep class com.f1pulse.app.domain.model.** { *; }
-keep class com.f1pulse.app.widget.WidgetState** { *; }

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
