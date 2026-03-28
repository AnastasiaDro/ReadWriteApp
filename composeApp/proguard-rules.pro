# App-specific R8 rules for Android release builds.

# Keep Room database/DAO metadata that may be referenced by generated code.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }

# Keep kotlinx.serialization generated serializers and companions.
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep enum names used in serialization and app logic.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
