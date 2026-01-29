# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/lib/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Firebase Crashlytics
-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.
-keep public class * extends java.lang.Exception  # Optional: Keep custom exceptions.

# If you are using custom keys in Crashlytics and they are being obfuscated (rare but possible with aggressive R8)
# -keep class com.google.firebase.crashlytics.** { *; }
