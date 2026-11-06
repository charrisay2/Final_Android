# ---------------------------------------------------------------------------
# R8 / ProGuard cho bản release
# ---------------------------------------------------------------------------

# Giữ tên file/đường dẫn để đọc stack trace (đối chiếu với mapping.txt)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- kotlinx.serialization -------------------------------------------------
# Navigation type-safe dùng @Serializable cho route, R8 có thể xoá serializer
# nếu không giữ lại. Giữ các thành viên sinh tự động của serializer.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.vinho.**$$serializer { *; }
-keepclassmembers class com.example.vinho.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.vinho.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Firebase --------------------------------------------------------------
# Firestore/Storage/Auth đã có consumer rules; giữ thêm lớp model phòng khi
# bạn chuyển sang @PropertyName/POJO mapping.
-keepclassmembers class com.example.vinho.data.model.** { *; }

# --- WorkManager -----------------------------------------------------------
# Worker được khởi tạo qua reflection bởi WorkManager
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# --- Tuỳ chọn gỡ log ở bản release ----------------------------------------
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
