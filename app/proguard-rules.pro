# Xposed entry points and hook code are loaded reflectively by the framework.
-keep class com.powermax.patchstudio.hook.** { *; }
-keep class com.powermax.patchstudio.core.** { *; }
-keep class com.powermax.patchstudio.data.ConfigProvider
-keep class com.powermax.patchstudio.data.StatusReceiver
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage { *; }
-dontwarn de.robv.android.xposed.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
