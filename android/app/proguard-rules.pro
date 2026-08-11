# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===== FadiVPN protection =====

# Keep Capacitor plugin and its public plugin methods.
-keep class com.fadi.vpn.FadiVpnPlugin { *; }

# Keep VPN service lifecycle.
-keep class com.fadi.vpn.FadiVpnService { *; }

# Keep Xray bridge/native entry points.
-keep class com.fadi.vpn.XrayRunner { *; }

# Keep native method names/signatures.
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep annotations required by Capacitor.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault

# Keep JSON classes used by the Xray configuration.
-keep class org.json.** { *; }

# Keep native libraries and JNI method linkage.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Remove unnecessary debug metadata from release builds.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

