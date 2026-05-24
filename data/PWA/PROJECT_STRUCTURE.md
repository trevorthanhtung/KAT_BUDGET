PWA/
|-- api/
|   `-- ty-gia.js
|      # API proxy cho Vercel, lấy XML tỷ giá từ Vietcombank

|-- public/
|   |-- fonts/
|   |     # Font Inter và Plus Jakarta Sans dùng giống app
|   |-- donates.webp
|   |     # Ảnh hộp thoại Donate
|   |-- favicon.svg
|   |-- icon-192.png
|   |-- icon-512.png
|   |-- icons.svg
|   `-- logo.webp
|         # Logo hộp thoại About

|-- src/
|   |-- components/
|   |   |-- AppChrome.tsx
|   |   |     # Header, thanh điều hướng dưới, banner cài PWA
|   |   |-- EChart.tsx
|   |   |     # Component biểu đồ ECharts dùng chung
|   |   |-- ExchangeRateDialog.tsx
|   |   |     # Hộp thoại tỷ giá quy đổi, gọi /api/ty-gia
|   |   |-- PinLockScreen.tsx
|   |   |     # Màn hình khóa bằng PIN
|   |   |-- PinSetupModal.tsx
|   |   |     # Hộp thoại tạo hoặc đổi PIN
|   |   `-- SettingsTab.tsx
|   |         # Màn hình cài đặt, không thêm sinh trắc học hay tự cập nhật PWA
|   |
|   |-- data/
|   |   `-- db.ts
|   |         # Schema Dexie và kiểu dữ liệu lưu trong IndexedDB
|   |
|   |-- hooks/
|   |   |-- useCalculations.ts
|   |   |     # Tính số dư, ngân sách, báo cáo, quy đổi tiền tệ
|   |   `-- useDatabase.ts
|   |         # Truy vấn Dexie realtime cho React
|   |
|   |-- locales/
|   |   |-- en.json
|   |   `-- vi.json
|   |         # Chuỗi giao diện đa ngôn ngữ
|   |
|   |-- types/
|   |   `-- index.ts
|   |         # Kiểu dữ liệu form, filter, backup
|   |
|   |-- utils/
|   |   |-- backup.ts
|   |   |     # Xuất/nhập file .kat tương thích backup Android
|   |   |-- constants.ts
|   |   |     # Danh sách tiền tệ, preset, giá trị mặc định
|   |   |-- helpers.ts
|   |   |     # Format tiền, nén backup, parse dữ liệu, quy đổi tiền
|   |   `-- transactions.ts
|   |         # Xử lý nhãn và hiển thị giao dịch
|   |
|   |-- App.css
|   |     # CSS chính cho layout mobile, modal, báo cáo, công cụ
|   |-- App.tsx
|   |     # Shell chính: state, dialog, luồng thêm/sửa/xóa dữ liệu
|   |-- i18n.ts
|   |     # Thiết lập i18next
|   |-- index.css
|   |     # Theme màu/font lấy theo Android
|   `-- main.tsx
|         # Điểm khởi chạy React

|-- .gitignore
|   # Bỏ qua dist, dev-dist, node_modules và log
|-- eslint.config.js
|-- index.html
|-- package-lock.json
|-- package.json
|-- PROJECT_STRUCTURE.md
|   # Tài liệu cấu trúc chính của PWA
|-- README.md
|-- RUN_DEV.ps1
|-- tsconfig.app.json
|-- tsconfig.json
|-- tsconfig.node.json
`-- vite.config.ts
    # Vite + React + VitePWA, service worker dev đã tắt để tránh lỗi dev-dist/sw.js

Ghi chú hiện tại:
- `dist/` và `dev-dist/` không giữ trong source. `dist/` chỉ sinh ra khi build, `dev-dist/` đã bị tắt trong dev.
- PWA dùng `registerType: 'prompt'`, không tự động ép cập nhật trong app.
- Nguồn tỷ giá Vietcombank đi qua `/api/ty-gia` để tránh lỗi CORS khi deploy Vercel.
- Màn hình Báo cáo đã khóa chiều rộng, ẩn scrollbar chip tiền tệ và ép chart/card không làm tràn khung mobile.
- Màn hình Tổng quan và modal thêm giao dịch đã được QA ở viewport mobile 390x844, không còn tràn ngang; numpad giao dịch đã thu gọn để phần form dễ thao tác hơn.
- Màn hình Vay nợ đã có lớp layout riêng cho tab, card, cột tiền và trạng thái; Vay nợ/Báo cáo đã được đo lại ở viewport mobile 390x844 và không làm bung ngang trang.
- Tài liệu phiên làm việc chỉ ghi tại file này, không dùng `NEXT_SESSION.md`.
