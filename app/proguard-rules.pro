# Add project specific ProGuard rules here.

# --- Room Persistence Library ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.RoomMasterTable { *; }

# --- Project Data Entities & Models ---
-keep class com.example.data.database.entity.** { *; }
-keep class com.example.data.database.dao.** { *; }
-keep class com.example.data.model.** { *; }
-keep class com.example.data.auth.** { *; }

# --- Kotlin Coroutines ---
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# --- OkHttp & Okio ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# --- Jetpack Compose ---
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
