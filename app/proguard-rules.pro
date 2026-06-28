# Keep classes that are instantiated via reflection
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep generic type information for serialization
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keepclasseswithmembers class androidx.compose.** {
    *** *(...);
}

# Room Database
-keep class androidx.room.** { *; }
-keepclasseswithmembers class * {
    @androidx.room.** <methods>;
}

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-keepclasseswithmembers class kotlinx.coroutines.** {
    *** *(...);
}

# NOTE: intentionally NOT blanket-keeping com.bernardo.feedvault.** — that would keep the
# desktop feature code in the play flavor. R8 must be free to strip the unreachable
# desktop classes (folded out by BuildConfig.ENABLE_DESKTOP = false).

# Room entities are referenced by generated code; keep them and their members to be safe.
-keep @androidx.room.Entity class com.bernardo.feedvault.data.** { *; }

# Kotlin
-dontwarn kotlin.**
-keepclassmembers class ** {
    *** lambda*(...);
}

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
