# The native library finds its entry point by class and method name,
# so R8 must not rename anything that declares a native method.
-keepclasseswithmembernames class * {
    native <methods>;
}
