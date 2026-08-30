# Aturan rilis. Hanya tambahkan keep-rule saat library terkait benar-benar masuk —
# jangan menambahkan aturan spekulatif.

# ---------------------------------------------------------------------------
# kotlinx.serialization (Fase 2)
#
# R8 tidak melihat referensi ke kelas $$serializer yang dibangkitkan compiler,
# jadi tanpa aturan ini serializer dibuang dan parsing gagal HANYA di build
# rilis — debug tetap hijau. Itulah kenapa assembleRelease masuk daftar
# perintah validasi Fase 2.
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses

# Simpan serializer yang dibangkitkan untuk setiap @Serializable.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# DTO kita sendiri: nama field dipakai sebagai kunci JSON saat tidak ada
# @SerialName, jadi field tidak boleh di-rename.
-keepclassmembers,allowobfuscation class com.agribicara.app.data.remote.**$$serializer { *; }
-keep,includedescriptorclasses class com.agribicara.app.data.remote.** { *; }

# ---------------------------------------------------------------------------
# Retrofit / OkHttp
# ---------------------------------------------------------------------------
-keepattributes Signature, Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
