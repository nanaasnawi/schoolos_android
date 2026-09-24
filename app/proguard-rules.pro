# School OS ProGuard Rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses,EnclosingMethod

# Kotlinx Serialization
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class **$$serializer {
    public static final **$$serializer INSTANCE;
}
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keep class * implements kotlinx.serialization.KSerializer { *; }
-dontwarn kotlinx.serialization.**

# App DTOs and Domain Models
-keep class com.schoolos.android.data.remote.dto.** { *; }
-keep class com.schoolos.android.data.remote.** { *; }
-keep class com.schoolos.android.core.chat.** { *; }
-keep class com.schoolos.android.domain.model.** { *; }

# Retrofit & OkHttp
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Coil Image Loading
-dontwarn coil.**
-keep class coil.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
