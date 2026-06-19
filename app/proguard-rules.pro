# ZeroCache ProGuard rules
-keep class com.zerocache.** { *; }
-keepattributes *Annotation*

# Keep hidden API interfaces used via reflection
-keep class android.content.pm.IPackageDataObserver { *; }
-keep class android.content.pm.IPackageDataObserver$Stub { *; }
-keep class * implements android.content.pm.IPackageDataObserver { *; }
