# Kế hoạch tiếp tục phát triển PWA cho KAT BUDGET

Dự án đang trong quá trình chuyển đổi KAT Budget thành Progressive Web App (PWA) và tinh chỉnh giao diện người dùng (UI/UX).
Các công việc chính liên quan đến PWA đã được thực hiện, hiện tại đang giải quyết các yêu cầu chi tiết về trải nghiệm người dùng.

## Mục tiêu hiện tại
Hoàn thiện và fix các lỗi UI/UX còn tồn đọng dựa trên phản hồi của người dùng ở phiên làm việc trước:
1. **Hiệu ứng lan tỏa (Ripple Effect):** Nút bấm hoặc thẻ thiếu hiệu ứng lan tỏa khi tương tác.
2. **Lỗi Xuất CSV:** Khi bấm vào "Xuất báo cáo CSV" xảy ra vấn đề (cần kiểm tra logic xuất file hoặc UI hiển thị bảng/modal).
3. **Bộ chọn thời gian (Calendar Picker):** Cần hiển thị lịch (datepicker) đàng hoàng khi chọn thời gian thay vì nhập text bình thường.
4. **Lỗi đơ ứng dụng:** Có thao tác (khả năng là liên quan đến xuất báo cáo hoặc lịch) gây đơ UI (freeze).
5. **Cập nhật Icon:** Đồng bộ các icon cho giống với thiết kế hoặc yêu cầu của người dùng.

## Các thành phần cần kiểm tra
- `Website/assets/css/style.css`: Thêm CSS cho hiệu ứng ripple, modal xuất CSV, và các icon mới.
- `Website/assets/js/main.js`: Sửa logic xuất CSV, khởi tạo bộ chọn lịch, và xử lý các sự kiện click để tránh lỗi đơ ứng dụng.
- `Website/index.html`: Cập nhật HTML cho các icon và bộ chọn lịch.

## Quy trình đề xuất
1. Giải quyết lỗi đơ ứng dụng (critical) trước.
2. Sửa tính năng xuất báo cáo CSV.
3. Tích hợp Datepicker (bộ chọn lịch).
4. Thêm hiệu ứng Ripple và thay đổi Icon.
