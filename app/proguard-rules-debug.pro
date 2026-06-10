# Debug-only ProGuard config.
#
# Why we run R8 in debug:
# Without shrinking, the unminified debug APK is ~73 MB because Compose
# Material Icons Extended alone contributes ~30+ MB of DEX (5000+ icon
# classes, each compiled into its own class). With R8 shrinking we drop
# that to the dozen or so icons actually referenced, taking the APK
# from 73 MB → ~12 MB.
#
# Why debug needs its own config:
# Release uses proguard-android-optimize.txt + obfuscation, which makes
# stacktraces unreadable and slows the build for no dev benefit. The
# rules below turn OFF those parts so debug retains:
#   - readable class/method names in logcat
#   - field/parameter names for debuggers
#   - line numbers in stacktraces
# while still benefiting from dead-code removal.

# Don't obfuscate. Keep every class/method/field name as-is so logcat
# stacktraces are immediately readable and breakpoints don't get
# remapped.
-dontobfuscate

# Don't run the optimizer. It's expensive and produces unstable bytecode
# that can confuse the debugger (inlined methods, dead-branch removal,
# etc.). Shrinking alone gives us 95% of the size win.
-dontoptimize

# Keep all source-file and line-number debug info — needed for
# meaningful stacktraces and step-debugging.
-keepattributes SourceFile, LineNumberTable
-keepattributes *Annotation*

# Compose @Preview functions are debug-time only but R8 doesn't know
# that. Keep them so Android Studio's Compose preview window still
# resolves them after the shrunk APK is installed for live-edit sessions.
-keep @androidx.compose.ui.tooling.preview.Preview class * { *; }
-keepclasseswithmembers class * {
    @androidx.compose.ui.tooling.preview.Preview <methods>;
}
