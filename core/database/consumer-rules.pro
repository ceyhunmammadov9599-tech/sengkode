# Consumer ProGuard rules for :core:database
#
# Merged automatically into the consuming app's R8 configuration.

# ---- Room entities --------------------------------------------------------
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

-keepclassmembers class * {
    @androidx.room.ColumnInfo <fields>;
    @androidx.room.PrimaryKey <fields>;
    @androidx.room.Embedded <fields>;
    @androidx.room.Relation <fields>;
}

# ---- Room database classes ------------------------------------------------
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    static ** INSTANCE;
    static ** Companion;
}

# ---- DAO interfaces -------------------------------------------------------
-keep @androidx.room.Dao class * { *; }

# ---- kotlinx.serialization generated serializers --------------------------
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
