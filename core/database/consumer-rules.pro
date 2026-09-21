# Consumer ProGuard rules for :core:database
#
# These rules are merged into the consuming app's R8 configuration
# automatically when the module is included as a dependency.

# ---- Room entities --------------------------------------------------------
# Preserve all classes annotated with @Entity so R8 does not remove
# fields that Room maps to database columns.
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# Keep @ColumnInfo, @PrimaryKey and @Embedded field names used by Room
# at runtime via reflection.
-keepclassmembers class * {
    @androidx.room.ColumnInfo <fields>;
    @androidx.room.PrimaryKey <fields>;
    @androidx.room.Embedded <fields>;
    @androidx.room.Relation <fields>;
}

# ---- Room database classes ------------------------------------------------
# Keep generated _Impl classes that Room creates at compile time.
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    static ** INSTANCE;
    static ** Companion;
}

# Keep DAO interfaces and their implementations.
-keep @androidx.room.Dao class * { *; }
-keep class * implements androidx.room.RoomDatabase { *; }

# ---- kotlinx.serialization generated serializers --------------------------
# Keep @Serializable classes declared inside this module and their
# generated companion serializers.
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    static ** Companion;
    static ** serializer(...);
    ** serializer();
    ** INSTANCE;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
