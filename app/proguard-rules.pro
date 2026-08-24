# Add project specific ProGuard / R8 rules here.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Room Database & SQLite
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.** { *; }
-keep class com.example.data.** { *; }

# Moshi / JSON Serialization
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}
-keep class com.squareup.moshi.** { *; }

# Kotlin Coroutines & Flow
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Android Jetpack DataStore & Preferences
-keep class androidx.datastore.** { *; }

# Security & Crypto - Preserve cryptographic engine and keystore
-keep class com.example.data.CryptoHelper { *; }
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-keep class android.security.keystore.** { *; }

# Security Hardening: Obfuscate identifiers and optimize bytecode
-repackageclasses ''
-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Play Services & Review / Update
-keep class com.google.android.play.core.** { *; }

