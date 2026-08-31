# ML Kit downloads models via Play services; keep its manifest-registered internals.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_translate.** { *; }
-dontwarn com.google.android.gms.**
