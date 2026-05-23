# KAT Budget Maintenance Guide

File nay de tra nhanh khi can sua loi hoac them tinh nang cho app KAT Budget.

## 1. Duoi file nghia la gi?

- `.kt`: code Kotlin. Dung de viet logic app, man hinh Compose, ViewModel, Room database, worker chay ngam, receiver Android.
- `.xml`: tai nguyen va cau hinh Android. Dung cho chuoi ngon ngu, theme, quyen/manifest, cau hinh backup, widget, FileProvider.
- `.kts`: Gradle Kotlin script. Dung de khai bao package, version app, SDK, dependency, build release.
- `.toml`: version catalog cua Gradle. Dung de gom version thu vien tai `gradle/libs.versions.toml`.

## 2. Cay thu muc chinh

- `app/src/main/java/com/katgr0up/katbudget/data/local`
  - `AppDatabase.kt`: cau hinh Room database, danh sach bang, version DB, ten file DB.
  - `dao/TransactionDao.kt`: tat ca cau lenh them, sua, xoa, lay data tu Room.
  - `entity/*.kt`: cau truc bang database. Them cot moi thi sua entity, tang version DB va them migration.
  - `type/CategoryType.kt`: hang so loai danh muc `INCOME` va `EXPENSE`.

- `app/src/main/java/com/katgr0up/katbudget/data/repository`
  - `TransactionRepository.kt`: lop trung gian giua DAO va ViewModel. Neu data nap len UI sai, kiem tra o day sau DAO.

- `app/src/main/java/com/katgr0up/katbudget/managers`
  - `BackupManager.kt`: xuat/nhap file backup `.kat`.
  - `CsvReportManager.kt`: tao noi dung bao cao CSV.
  - `ExchangeRateManager.kt`: lay va cache ty gia.
  - `PreferencesManager.kt`: luu cai dat nho bang SharedPreferences.
  - `AppUpdater.kt`: kiem tra JSON cap nhat, tai APK, mo man hinh cai dat APK.

- `app/src/main/java/com/katgr0up/katbudget/viewmodel`
  - `BudgetViewModel.kt`: logic chinh cua giao dich, nguon tien, ngan sach, danh muc.
  - `DebtViewModel.kt`: logic khoan no/cho vay.
  - `GoalViewModel.kt`: logic muc tieu tiet kiem.
  - `SettingsViewModel.kt`: logic cai dat, PIN, backup tu dong, ty gia, cap nhat app.

- `app/src/main/java/com/katgr0up/katbudget/ui`
  - `components`: UI dung lai nhieu noi, mau sac, card, button, chart, bottom navigation.
  - `dialogs`: popup nhap/sua data.
  - `screens`: man hinh bao boc lon nhu splash, PIN lock, dashboard.
  - `tabs`: cac tab chinh trong dashboard.
  - `tools`: man hinh cong cu phu nhu ngan sach, muc tieu, giao dich dinh ky.
  - `theme/Theme.kt`: Material theme, mau sang/toi, edge-to-edge.
  - `utils`: formatter tien/ngay, hang so UI, animation.

- `app/src/main/java/com/katgr0up/katbudget/workers`
  - `AutoBackupWorker.kt`: tien trinh auto backup chay nen bang WorkManager.

- `app/src/main/java/com/katgr0up/katbudget`
  - `MainActivity.kt`: diem mo app, theme, ViewModel, cap quyen thong bao, crash backup guard.
  - `DailyReminderScheduler.kt`: len lich thong bao hang ngay.
  - `NotificationReceiver.kt`: nhan lich va hien notification.
  - `BootReceiver.kt`: dat lai lich thong bao sau khi reboot/cap nhat app.
  - `UpdateReceiver.kt`: nhan su kien DownloadManager tai APK xong.

- `app/src/main/java/com/katgr0up/katbudget/widget`
  - `KatBudgetWidgetProvider.kt`: logic widget ngoai man hinh chinh.

## 3. Resource XML nen sua o dau?

- `app/src/main/AndroidManifest.xml`: package runtime, quyen Android, activity, receiver, provider.
- `app/src/main/res/values/strings.xml`: tieng Viet mac dinh.
- `app/src/main/res/values-en/strings.xml`: tieng Anh. Them key moi thi phai them ca hai file.
- `app/src/main/res/values/colors.xml`: mau XML truyen thong.
- `app/src/main/res/values/themes.xml`: theme Android goc truoc khi Compose render.
- `app/src/main/res/xml/provider_paths.xml`: FileProvider, dung khi mo/cai APK update.
- `app/src/main/res/xml/backup_rules.xml`: cau hinh Android Auto Backup.
- `app/src/main/res/xml/data_extraction_rules.xml`: cau hinh backup/restore Android 12+.
- `app/src/main/res/xml/widget_info.xml`: kich thuoc va layout widget.
- `app/src/main/res/layout/widget_layout.xml`: giao dien widget RemoteViews.

## 4. Khoanh vung loi nhanh

- UI lech, nut bam khong dung: xem `ui/components`, `ui/tabs`, `ui/tools`, `ui/screens`.
- Popup nhap lieu sai: xem `ui/dialogs`.
- Sai so tien, tong thu/chi, ngan sach: xem `viewmodel/BudgetViewModel.kt`, `data/repository/TransactionRepository.kt`, `data/local/dao/TransactionDao.kt`.
- App khong luu du lieu: xem `AppDatabase.kt`, entity lien quan, DAO.
- Backup `.kat` loi: xem `managers/BackupManager.kt`, `workers/AutoBackupWorker.kt`, `ui/screens/DashboardScreen.kt`.
- CSV loi: xem `managers/CsvReportManager.kt`, `ui/dialogs/ExportCsvDialog.kt`, `ui/screens/DashboardScreen.kt`.
- Thong bao khong hien: xem `MainActivity.kt`, `DailyReminderScheduler.kt`, `NotificationReceiver.kt`, `AndroidManifest.xml`.
- Cap nhat APK loi: xem `managers/AppUpdater.kt`, `UpdateReceiver.kt`, `provider_paths.xml`, quyen `REQUEST_INSTALL_PACKAGES`.
- Doi ten app/chuoi UI: sua `strings.xml` va `values-en/strings.xml`, han che hard-code trong `.kt`.
- Doi package/app id: sua `app/build.gradle.kts`, `AndroidManifest.xml`, package Kotlin, FileProvider authority, ProGuard.

## 5. Luu y quan trong khi phat hanh

- Khong dung `fallbackToDestructiveMigration()` cho ban release vi co the xoa data nguoi dung khi database doi schema.
- Khi them/sua cot Room: tang `version` trong `AppDatabase.kt`, cap nhat schema, viet migration neu app da phat hanh.
- Khi tang ban cap nhat APK: tang `versionCode`; `versionName` chi de nguoi dung doc.
- APK dua len web/Google Drive phai la APK da sign release, khong dua `app-release-unsigned.apk` cho nguoi dung.
- Neu them string moi, chay check de dam bao `values` va `values-en` khop key.

## 6. Lenh kiem tra nen chay truoc khi gui APK

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleRelease :app:lintDebug --warning-mode all
```

Kiem tra branding cu:

```powershell
rg -n "Kat Wallet|KATWallet|katwallet|KAT Wallet|KAT WALLET|vm_wallet_" app/src app/build.gradle.kts
```

Kiem tra quyen/doc file anh:

```powershell
rg -n "READ_MEDIA|READ_EXTERNAL|requestPermissions" app/src
```
