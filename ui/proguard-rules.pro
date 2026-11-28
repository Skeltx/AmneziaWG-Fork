# sing-box-lx is a gomobile library: JNI resolves classes and methods by name.
-keep class io.nekohasekai.libbox.** { *; }
-keep class go.** { *; }
-keepclassmembers class * implements io.nekohasekai.libbox.PlatformInterface { *; }
-keepclassmembers class * implements io.nekohasekai.libbox.CommandServerHandler { *; }
-dontwarn io.nekohasekai.libbox.**
-dontwarn go.**
