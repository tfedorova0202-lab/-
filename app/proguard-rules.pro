# ProGuard / R8 rules for release builds and app store distribution

# Preserve generic signatures, annotations, and Kotlin metadata for reflection/serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keep class kotlin.Metadata { *; }

# Keep Kotlinx Serialization generated serializers & companion objects
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    public static *** serializer(...);
}
-keep,includedescriptorclasses class com.example.api.**$$serializer { *; }
-keepclassmembers class com.example.api.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.api.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep API DTOs, Retrofit interfaces, and Firestore/Room entity models (incl. getters/setters/no-arg ctors)
-keep class com.example.api.** { *; }
-keep interface com.example.api.** { *; }
-keep class com.example.data.** { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keep @com.google.firebase.firestore.IgnoreExtraProperties class * { *; }

# Retrofit & OkHttp
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**

# Transitive Gson (used by Firebase/Firestore internals)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * implements com.example.data.DebtDao { *; }
-keep class * implements com.example.data.MessageDao { *; }
-keep class * implements com.example.data.QueryHistoryDao { *; }
-keep class * implements com.example.data.LegalTemplateDao { *; }

# Coroutines & Lifecycle
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Strip verbose/debug/info logs in release builds to prevent runtime PII leakage via Logcat
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}


