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
-keep class kotlinx.coroutines.** { *; }

# Google Play Core & In-App Updates
-keep class com.google.android.play.core.appupdate.** { *; }
-keep interface com.google.android.play.core.appupdate.** { *; }
-keep class com.google.android.play.core.install.** { *; }
-keep interface com.google.android.play.core.install.** { *; }
-keep class com.google.android.play.core.common.** { *; }
-keep class com.google.android.play.core.tasks.** { *; }
-dontwarn com.google.android.play.core.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Dagger Core & Hilt Protection
-keep class dagger.** { *; }
-keep interface dagger.** { *; }
-keep class dagger.hilt.** { *; }
-keep interface dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-dontwarn dagger.**
-dontwarn dagger.hilt.**

# Javax & Jakarta Inject
-keep class javax.inject.** { *; }
-keep interface javax.inject.** { *; }
-keep class jakarta.inject.** { *; }
-keep interface jakarta.inject.** { *; }

# ViewModels and Hilt Modules - Never Strip or Inline
-keep class * extends androidx.lifecycle.ViewModel {
    @javax.inject.Inject <init>(...);
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}
-keep class **_HiltModules* { *; }
-keep class **_Factory { *; }
-keep class * implements dagger.internal.Factory { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * {
    @javax.inject.Inject <init>(...);
    <init>(...);
}

# AndroidX Navigation & Lifecycle Compose
-keep class androidx.navigation.** { *; }
-keep interface androidx.navigation.** { *; }
-keep class androidx.hilt.navigation.** { *; }
-keep class androidx.hilt.navigation.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }
-keep class androidx.compose.runtime.** { *; }

# Keep ALL School OS Application Classes & Members from being stripped
-keep class com.schoolos.android.** { *; }
-keepclassmembers class com.schoolos.android.** { *; }
