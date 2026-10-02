# ProGuard rules for release builds and app store distribution

# Keep Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembers class * {
    companion object;
}
-keep class kotlinx.serialization.json.** { *; }

# Keep API and data models
-keep class com.example.api.** { *; }
-keep interface com.example.api.** { *; }
-keep class com.example.data.** { *; }

# Retrofit & OkHttp
-keepattributes Signature, EnclosingMethod
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.example.data.** { *; }
-keep class * implements com.example.data.DebtDao { *; }

# Coroutines & Lifecycle
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}


