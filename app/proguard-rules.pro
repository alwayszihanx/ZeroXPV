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
-dontobfuscate
-keep,allowoptimization class is.xyz.mpv.** { public protected *; }
-keep,allowoptimization class net.mediaarea.mediainfo.lib.** { public protected *; }
-dontwarn org.xmlpull.v1.**
-dontnote org.xmlpull.v1.**
-dontwarn org.slf4j.impl.StaticLoggerBinder

# SMBJ ProGuard Rules - Targeted keeps for used classes only
-keep class com.hierynomus.smbj.SMBClient { *; }
-keep class com.hierynomus.smbj.SmbConfig { *; }
-keep class com.hierynomus.smbj.auth.AuthenticationContext { *; }
-keep class com.hierynomus.smbj.connection.Connection { *; }
-keep class com.hierynomus.smbj.session.Session { *; }
-keep class com.hierynomus.smbj.share.DiskShare { *; }
-keep class com.hierynomus.smbj.transport.tcp.async.AsyncDirectTcpTransportFactory { *; }
-keep class com.hierynomus.msdtyp.AccessMask { *; }
-keep class com.hierynomus.mssmb2.SMB2CreateDisposition { *; }
-keep class com.hierynomus.mssmb2.SMB2ShareAccess { *; }
-keep class com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation { *; }

# JGSS (Kerberos/SPNEGO) - Optional, not needed for basic NTLM auth
# These are used for domain authentication, which we don't use
-dontwarn org.ietf.jgss.**
-dontnote org.ietf.jgss.**

# MBassador (event bus used by SMBJ) - EL (Expression Language) is optional
-dontwarn net.engio.mbassy.dispatch.el.**
-dontnote net.engio.mbassy.dispatch.el.**
-dontwarn javax.el.**
-dontnote javax.el.**

# MBassador (event bus used by SMBJ) - keep core classes only
-keep class net.engio.mbassy.bus.** { *; }
-keep class net.engio.mbassy.listener.** { *; }
-dontwarn net.engio.mbassy.dispatch.el.**
-dontnote net.engio.mbassy.dispatch.el.**

# BouncyCastle (crypto provider used by SMBJ) - keep provider registration only
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }
-keep class org.bouncycastle.jcajce.provider.** { *; }
-dontwarn org.bouncycastle.**

# ASN.1 classes - keep only what's used
-keep class com.hierynomus.asn1.Asn1Object { *; }
-keep class com.hierynomus.asn1.structures.** { *; }

# Keep all classes that use reflection or are loaded dynamically
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
