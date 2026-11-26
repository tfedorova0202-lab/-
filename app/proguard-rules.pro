# ProGuard / R8 rules for release builds and app store distribution

# Preserve generic signatures, annotations, and Kotlin metadata for reflection/serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keep class kotlin.Metadata { *; }

# Keep Kotlinx Serialization generated serializers & annotated DTOs
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    public static *** serializer(...);
}
-keep,includedescriptorclasses class com.example.api.**$$serializer { *; }
-keep @kotlinx.serialization.Serializable class com.example.api.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.example.api.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Retrofit service interfaces (endpoints only, allowing internal managers to obfuscate)
-keep interface com.example.api.GeminiApiService { *; }
-keep interface com.example.api.ExternalLegalApiService { *; }

# Keep Firestore models (@Keep / @IgnoreExtraProperties / @PropertyName) and Room entities/DAOs
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keep @com.google.firebase.firestore.IgnoreExtraProperties class * { *; }
-keep @androidx.room.Entity class * { *; }

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

# Note: AGP automatically configures -printseeds, -printusage, -printmapping, and
# -printconfiguration into outputs/mapping/release/ when isMinifyEnabled = true.

# Strip verbose/debug/info logs and zero-arg printStackTrace() in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}


