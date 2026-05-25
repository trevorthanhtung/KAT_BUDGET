# Codex Usage Handoff

- Timestamp: 2026-05-23 23:38:15 +07:00
- Workspace chính: `D:\02_PROJECTS\4_KAT BUDGET\data\PWA`
- Trạng thái gần nhất:
  - Đã sync string Android (`values/strings.xml`, `values-en/strings.xml`) sang:
    - `D:\02_PROJECTS\4_KAT BUDGET\data\PWA\src\locales\vi.json`
    - `D:\02_PROJECTS\4_KAT BUDGET\data\PWA\src\locales\en.json`
  - Đã refactor `App.tsx` để bỏ hết `lang === 'vi' ? ... : ...` và dùng `t('key')`.
  - Đã làm thêm UI parity pass cho PWA theo Android gốc:
    - `src/index.css`: token màu/nền theo `BudgetColors.kt` (`#F6F8FA`, `#EAFBF2`, `#DDE5E0`, dark `#0D1117/#161B22`, accent dark `#4ADE80`), bỏ nền radial và đổi sang gradient dọc kiểu Android.
    - `src/App.css`: thêm block `Android UI parity pass` để ép app shell mobile-first 520px, topbar, AppCard radius 16, Empty/Card radius 24, settings rows, chips, modal, switch, bottom nav/FAB theo style Android.
    - `src/App.tsx`: gắn class cho các màn tool `Budget`, `Recurring`, `Goal`, `Category`; dashboard asset card, account card, transaction row; thêm class cho `Debts`/`Reports` (`tab-screen`, `segment-control`, `empty-state-card`, `report-card`) để CSS override ổn định hơn.
  - Đã sửa riêng dark mode cho sát Android hơn:
    - `src/index.css`: dark tokens theo `BudgetColors.kt`: background `#0D1117`, mid gradient `#0F2A22`, card/nav `#161B22`, text `#F8FAFC`, subtext `#9CA3AF`, border `rgba(255,255,255,.12)`, accent `#4ADE80`.
    - `src/App.css`: thêm `Android dark mode parity` và `Final dark overrides` để card/nav/modal/input/chip/inline hard-code green/red không bị sáng hoặc lệch tông trong dark mode.
  - Build đã pass ở `D:\02_PROJECTS\4_KAT BUDGET\data\PWA` với `npm run build`.
  - `git status` từ thư mục PWA đang thấy `data\PWA` là untracked (`?? ./`) và vẫn có thay đổi cũ ở `data\IOS`; đừng revert `IOS` nếu không có yêu cầu riêng.

## Note cho Antigravity

Nếu Codex dừng vì usage, tiếp tục theo thứ tự dưới đây.

### 1. Chốt UI parity trước khi qua UX

Mục tiêu hiện tại: PWA phải nhìn giống Android gốc nhất có thể trong cả light mode và dark mode.

Nguồn tham chiếu Android:
- `D:\02_PROJECTS\4_KAT BUDGET\data\KAT Budget\app\src\main\java\com\katgr0up\katbudget\ui\components\BudgetColors.kt`
- `D:\02_PROJECTS\4_KAT BUDGET\data\KAT Budget\app\src\main\java\com\katgr0up\katbudget\ui\theme\Theme.kt`
- `D:\02_PROJECTS\4_KAT BUDGET\data\KAT Budget\app\src\main\java\com\katgr0up\katbudget\ui\components\AppCards.kt`
- `D:\02_PROJECTS\4_KAT BUDGET\data\KAT Budget\app\src\main\java\com\katgr0up\katbudget\ui\components\SharedUI.kt`
- `D:\02_PROJECTS\4_KAT BUDGET\data\KAT Budget\app\src\main\java\com\katgr0up\katbudget\ui\tabs\SettingsTabComponents.kt`

Các điểm phải rà:
- Dashboard: asset summary card, account cards, recent transaction rows, search input.
- Debts: segmented control, debt rows, paid/unpaid pill.
- Reports: month card, filter chips, report cards, chart card spacing, list rows.
- Settings: section header, settings card, row height/padding, subtitle typography, switch.
- Tool screens: Budget, Recurring, Goal, Category top header/back button, summary cards, empty states, rows.
- Modals: transaction bottom sheet, source/debt/budget/goal/recurring dialogs, CSV/about/support/donate dialogs.
- Dark mode: không để nền/card/input bị quá sáng; đúng tông `#0D1117 -> #0F2A22 -> #0D1117`, card `#161B22`, border trắng alpha 12%, accent `#4ADE80`.

Lệnh test:
```powershell
cd "D:\02_PROJECTS\4_KAT BUDGET\data\PWA"
npm run build
npm run dev -- --host 0.0.0.0 --port 5173
```

URL kiểm tra: `http://127.0.0.1:5173`

### 2. UX task sau khi UI đã sát

Sau khi UI ổn, chuyển sang UX theo thứ tự này:

1. Transaction flow:
   - Mở FAB phải vào form thêm giao dịch nhanh, ít bước nhất.
   - Chọn loại Chi tiêu/Thu nhập/Chuyển tiền rõ ràng.
   - Chọn tài khoản, danh mục, tiền tệ, ghi chú, ngày, ảnh hóa đơn.
   - Calculator/numpad phải dễ dùng bằng một tay, nút lưu rõ.
   - Sau khi lưu phải có feedback rõ và đóng modal đúng.

2. Account/source flow:
   - Tạo/sửa tài khoản, loại tài khoản, số dư ban đầu, tiết kiệm/lãi suất.
   - Kiểm tra empty state khi chưa có tài khoản.
   - Không cho tạo giao dịch nếu chưa có nguồn tiền hợp lệ, hoặc phải hướng người dùng tạo nguồn.

3. Category flow:
   - Tạo/sửa/xóa category.
   - Filter Income/Expense/All rõ.
   - Khi xóa category đang dùng trong transaction/budget phải UX rõ: không mất dữ liệu lạ, hiển thị `misc_deleted_category` nếu cần.

4. Budget flow:
   - Tạo budget theo category/tháng/currency.
   - Hiển thị over-budget warning rõ.
   - Progress bar và percent phải dễ đọc trong dark mode.

5. Recurring flow:
   - Tạo/sửa giao dịch định kỳ.
   - Ngày trong tháng, amount, category, source phải validate.
   - Empty state và schedule list phải rõ.

6. Goal flow:
   - Tạo/sửa mục tiêu tiết kiệm.
   - Progress tổng và progress từng goal phải nhất quán với Android.

7. Reports flow:
   - Filter tháng/loại/currency.
   - Chart và card phải không che chữ, không overflow ở mobile.
   - Empty/no-data state phải có.

8. Settings/support data flow:
   - Backup `.kat`, restore `.kat/.json`, CSV export.
   - Theme toggle, PIN lock, language toggle.
   - Support/about/donate dialogs.

### 3. Những tính năng PWA không thể hoặc khó thay thế Android native

PWA khó làm giống Android hoàn toàn:
- Background service/native scheduler ổn định như Android để tự chạy recurring khi app không mở.
- Widget Android home screen như native app.
- Native notification/background alarm đáng tin cậy nếu browser bị kill hoặc OS giới hạn.
- File system native full quyền; PWA chỉ dùng picker/download/browser storage.
- Android biometric/PIN native thật; PWA hiện chỉ có PIN lưu local.
- Native share sheet/camera/storage sâu như Android; PWA chỉ có mức browser API hỗ trợ.

UX thay thế nên làm trong PWA:
- Khi mở app, tự kiểm tra recurring chưa chạy và gợi ý/tự ghi nhận giao dịch định kỳ.
- Thêm banner/reminder trong app thay vì phụ thuộc background notification.
- Backup/restore rõ hơn, có trạng thái thành công/thất bại.
- Export/import đặt ở Settings dễ tìm.
- Dùng install prompt PWA và chỉ dẫn ngắn khi browser hỗ trợ.

### 4. Quy tắc khi tiếp tục sửa

- Đừng revert `data\IOS`; thư mục này còn thay đổi cũ và có thể không liên quan.
- Làm trên `D:\02_PROJECTS\4_KAT BUDGET\data\PWA`.
- Ưu tiên CSS/class nhỏ, tránh refactor lớn `App.tsx` nếu không cần.
- Sau mỗi cụm sửa phải chạy `npm run build`.
- Nếu sửa string mới, thêm vào cả `src/locales/vi.json` và `src/locales/en.json`, không quay lại kiểu `lang === 'vi' ? ... : ...`.
