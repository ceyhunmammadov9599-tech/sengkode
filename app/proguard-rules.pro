# SENGKODE release rules.
#
# Privacy-first contract: no network code, no analytics, no logging of
# user content. R8 minification is ON from Phase 0; every phase gate
# re-verifies assembleRelease.

# Hilt, Room, Compose and Navigation ship consumer rules; nothing
# extra is required for the Phase 0 surface.

-dontwarn org.codehaus.mojo.animal_sniffer.*

# ---- ZXing ----------------------------------------------------------------
# Keep all ZXing classes used by ZxingQrEngine at runtime.
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**

# ---- kotlinx.serialization ------------------------------------------------
# Keep the serialization runtime and all generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *; }

# Keep @Serializable annotated classes and their companions.
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
