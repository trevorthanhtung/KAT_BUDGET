# Quy tắc ProGuard cho KAT Budget

# Giữ thông tin dòng code để dễ đọc crash log khi app lỗi bản release.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Giữ các Entity, DAO, Database của Room để tránh lỗi khi tối ưu code.
-keep class com.katgr0up.katbudget.data.local.** { *; }

# Giữ Repository và ViewModel.
-keep class com.katgr0up.katbudget.data.** { *; }
-keep class com.katgr0up.katbudget.viewmodel.** { *; }

# Giữ các class được Android gọi trực tiếp.
-keep class com.katgr0up.katbudget.BootReceiver { *; }
-keep class com.katgr0up.katbudget.NotificationReceiver { *; }
-keep class com.katgr0up.katbudget.UpdateReceiver { *; }
-keep class com.katgr0up.katbudget.MainActivity { *; }
-keep class com.katgr0up.katbudget.workers.** { *; }

# Giữ metadata Kotlin và annotation.
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*
