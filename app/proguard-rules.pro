# SENGKODE release rules.
#
# Privacy-first contract: no network code, no analytics, no logging of
# user content. R8 minification is ON from Phase 0; every phase gate
# re-verifies assembleRelease.

-dontwarn org.codehaus.mojo.animal_sniffer.*

# ---- ZXing ----------------------------------------------------------------
# Prevent R8 from removing QR encode/decode classes used by ZxingQrEngine.
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**

# ---- kotlinx.serialization ------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *; }

# Keep @Serializable-annotated classes and their generated companions.
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
