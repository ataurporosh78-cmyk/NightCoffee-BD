# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve @Keep and Firestore/Moshi/Room data models for release minification
-keep class com.example.model.** { *; }
-keepclassmembers class com.example.model.** { *; }
-keep class com.example.data.local.** { *; }
-keepclassmembers class com.example.data.local.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations
