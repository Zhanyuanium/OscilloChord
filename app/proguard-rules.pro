# Keep Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep data classes used by DataStore
-keepclassmembers class * {
    @androidx.datastore.preferences.core.PreferenceKey <fields>;
}
