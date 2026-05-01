# Keep kotlinx.serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep custom serializers / @Serializable classes
-keep,includedescriptorclasses class com.higumasoft.mneme.**$$serializer { *; }
-keepclassmembers class com.higumasoft.mneme.** {
    *** Companion;
}
-keepclasseswithmembers class com.higumasoft.mneme.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Google API client
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-dontwarn com.google.api.client.**
