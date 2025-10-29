# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Python-related classes
-keep class com.github.xmbox.pyramid.** { *; }
-keep class com.chaquo.python.** { *; }

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
