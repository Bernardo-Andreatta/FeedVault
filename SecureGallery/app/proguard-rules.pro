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

# Keep our application classes
-keep class com.example.securegallery.** { *; }
-keepclasseswithmembers class com.example.securegallery.** {
    *** *(...);
}

# Keep data classes
-keep class com.example.securegallery.data.** { *; }

# Kotlin specific rules
-keepclasses class kotlin.** { *; }
-keepclassmembers class ** {
    *** lambda*(...);
}

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
