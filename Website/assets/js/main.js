document.addEventListener("DOMContentLoaded", () => {
    document.body.classList.add('js-enabled');

    // === 0. MICRO-INTERACTION UTILITIES ===
    const haptic = (ms = 30) => { try { navigator.vibrate && navigator.vibrate(ms); } catch(e) {} };
    let _audioCtx;
    const microClick = () => {
        try {
            if (!_audioCtx) _audioCtx = new (window.AudioContext || window.webkitAudioContext)();
            const o = _audioCtx.createOscillator();
            const g = _audioCtx.createGain();
            o.type = 'sine';
            o.frequency.setValueAtTime(1800, _audioCtx.currentTime);
            g.gain.setValueAtTime(0.03, _audioCtx.currentTime);
            g.gain.exponentialRampToValueAtTime(0.001, _audioCtx.currentTime + 0.06);
            o.connect(g).connect(_audioCtx.destination);
            o.start();
            o.stop(_audioCtx.currentTime + 0.06);
        } catch(e) {}
    };
    
    // === 1. XỬ LÝ THEME ===
    const themeBtn = document.getElementById('theme-btn');
    const icon = themeBtn ? themeBtn.querySelector('i') : null;
    const html = document.documentElement;
    let isDarkMode = localStorage.getItem('theme') === 'dark';
    
    const applyTheme = (dark) => {
        if (dark) {
            html.setAttribute('data-theme', 'dark');
            if (icon) icon.className = 'fa-solid fa-sun';
        } else {
            html.removeAttribute('data-theme');
            if (icon) icon.className = 'fa-solid fa-moon';
        }
    };
    applyTheme(isDarkMode);
    
    if (themeBtn) {
        themeBtn.addEventListener('click', () => {
            isDarkMode = !isDarkMode;
            applyTheme(isDarkMode);
            localStorage.setItem('theme', isDarkMode ? 'dark' : 'light');
            haptic();
            microClick();
        });
    }

    // === 2. XỬ LÝ NGÔN NGỮ ===
    const dictionary = {
        vi: {
            metaDesc: "KAT Budget giúp ghi thu chi, lập ngân sách và xem báo cáo dòng tiền. Dữ liệu tài chính được lưu cục bộ trên thiết bị.",
            pageTitle: "KAT Budget - Quản lý tiền cá nhân | KAT GR0UP",
            privacyPageTitle: "Chính sách quyền riêng tư - KAT Budget | KAT GR0UP",
            privacyMetaDesc: "KAT Budget không yêu cầu đăng nhập, lưu dữ liệu tài chính cục bộ và chỉ dùng mạng để cập nhật tỷ giá khi cần.",
            installPageTitle: "Hướng dẫn cài đặt - KAT Budget | KAT GR0UP",
            installMetaDesc: "Hướng dẫn cài đặt KAT Budget trên Android và iOS.",
            heroBadge: "Gọn gàng hơn cho dòng tiền của bạn",
            heroTitle1: "Thấu hiểu dòng tiền.",
            heroTitle2: "Quản lý thông minh.",
            heroDesc: "Ghi thu chi, lập ngân sách và xem báo cáo dòng tiền trong một trải nghiệm gọn, rõ và dễ theo dõi.",
            btnDownload: "Tải Ứng Dụng",
            btnDownloadAndroid: "Tải ngay",
            btnWebApp: "Web App",
            btnInstallGuide: "Hướng dẫn cài đặt",
            btnShare: "Chia sẻ",
            webAppModalTitle: "Lưu ý & Hướng dẫn (iOS)",
            webAppModalDesc: "Web App đa nền tảng đang trong quá trình phát triển nên có thể chưa hoàn thiện 100%.",
            webAppModalGuideTitle: "Cài đặt lên màn hình chính iOS:",
            webAppModalGuide1: "Mở đường link bằng trình duyệt <strong>Safari</strong>.",
            webAppModalGuide2: "Nhấn vào biểu tượng <strong>Chia sẻ</strong> ở dưới cùng.",
            webAppModalGuide3: "Chọn <strong>Thêm vào MH chính</strong>.",
            webAppModalBtn: "Mở Web App ngay",
            btnSubtext: "Kiểm tra APK trên VirusTotal",
            btnVersion: "Phiên bản v1.0.3",
            downloadVersionLabel: "Phiên bản",
            downloadSizeLabel: "Dung lượng",
            downloadPlatformLabel: "Nền tảng",
            downloadPlatformValue: "Android 11+ · Web App (Đa nền tảng)",
            downloadUpdatedLabel: "Cập nhật",
            webAppComingSoonText: "Web App đa nền tảng đang được hoàn thiện. Bạn chờ chút nhé!",
            devBy: "Phát triển bởi",
            devSupport: "Hỗ Trợ",
            guaranteeTitle: "Dữ liệu lưu cục bộ trên thiết bị",
            guaranteeDesc: "KAT Budget không yêu cầu tài khoản và không vận hành máy chủ đồng bộ dữ liệu. Số dư, giao dịch, ngân sách và khoản vay được lưu trong bộ nhớ ứng dụng trên thiết bị; dữ liệu chỉ rời thiết bị khi bạn chủ động xuất, sao lưu hoặc chia sẻ file.",
            releaseKicker: "Phiên bản 1.0.3",
            releaseTitle: "Cập nhật mới có gì?",
            releaseDesc: "Bản cập nhật này nâng cấp widget và luồng tạo tài khoản đa tiền tệ.",
            releaseItem1: "Thêm số dư ban đầu đa tiền tệ khi tạo tài khoản.",
            releaseItem2: "Widget hiển thị số dư, dòng tiền tháng và giao dịch gần nhất.",
            releaseItem3: "Nút nhanh Thu/Chi trên widget mở đúng loại giao dịch trong app.",
            archiveKicker: "Lưu trữ APK",
            archiveTitle: "Các phiên bản cũ",
            archiveDesc: "Bạn nên dùng bản mới nhất để nhận đầy đủ bản sửa lỗi và cải thiện giao diện. Các phiên bản cũ được lưu lại để đối chiếu hoặc cài lại khi thật sự cần.",
            archiveButton: "Mở thư mục phiên bản cũ",
            featHeader: "Tính Năng Cốt Lõi",
            feat1Title: "Ghi chép nhanh chóng",
            feat1Desc: "Ghi thu, chi hoặc chuyển tiền giữa các tài khoản với số tiền, ghi chú, ngày giao dịch và loại tiền rõ ràng.",
            feat2Title: "Báo cáo trực quan",
            feat2Desc: "Xem dòng tiền theo kỳ, loại tiền và danh mục bằng biểu đồ dễ đọc, đủ để nhận ra khoản nào đang ảnh hưởng nhiều nhất.",
            feat3Title: "Lập ngân sách",
            feat3Desc: "Theo dõi hạn mức theo danh mục và mục tiêu tiết kiệm để điều chỉnh chi tiêu trước khi vượt kế hoạch.",
            feat4Title: "Khoản vay rõ ràng",
            feat4Desc: "Tách riêng khoản cho vay và đi vay, kèm số tiền, người liên quan, hạn thanh toán và trạng thái đã thu hoặc đã trả.",
            feat5Title: "Sao lưu và xuất dữ liệu",
            feat5Desc: "Tạo file sao lưu .kat để khôi phục khi đổi máy hoặc cài lại ứng dụng. Khi cần đối chiếu, bạn có thể xuất báo cáo CSV theo thời gian, tài khoản hoặc loại giao dịch.",
            ratesKicker: "Tỷ giá tự động",
            ratesTitle: "Tỷ giá ngoại tệ tự cập nhật",
            ratesDesc: "Khi có mạng, KAT Budget lấy tỷ giá tham khảo từ Vietcombank để quy đổi về VND. Dữ liệu tài chính vẫn lưu trên thiết bị của bạn.",
            ratesBaseLabel: "Mốc quy đổi",
            ratesNoteBefore: "Nguồn dữ liệu:",
            ratesNoteAfter: "Tỷ giá chỉ dùng để tham khảo trong app, không phải giá giao dịch thời gian thực.",
            cpKicker: "Web App Đa Nền Tảng",
            cpTitle: "Trải nghiệm trên mọi thiết bị",
            cpDesc: "KAT Budget là một Web App hoàn chỉnh. Bạn có thể sử dụng mượt mà trên trình duyệt của macOS, Windows hay Linux mà không cần cài đặt.",
            previewHeader: "Luồng Thao Tác Trong App",
            previewKicker: "Thao tác thường dùng",
            previewTitle: "Theo dõi bằng biểu đồ, xuất dữ liệu khi cần",
            previewDesc: "KAT Budget trình bày dòng tiền bằng biểu đồ dễ đọc và hỗ trợ xuất CSV để bạn tự lưu trữ hoặc đối chiếu dữ liệu khi cần.",
            previewPoint1: "Biểu đồ dòng tiền",
            previewPoint2: "Xuất file CSV",
            previewPoint3: "Tự lưu trữ dữ liệu",
            faqHeader: "Câu Hỏi Thường Gặp",
            faq1Q: "Có thể kiểm tra file APK trước khi cài không?",
            faq1A: "Có. File APK tải từ website có liên kết",
            faq1B: "để bạn tự đối chiếu kết quả quét trước khi cài đặt. Dữ liệu tài chính được lưu cục bộ; app không yêu cầu tài khoản và không vận hành máy chủ đồng bộ dữ liệu.",
            faq2Q: "Cài đặt APK trên Android như thế nào?",
            faq2A: "Bấm nút Tải ứng dụng, mở file APK sau khi tải xong và làm theo hướng dẫn trên màn hình. Nếu Android hỏi quyền cài ứng dụng từ nguồn hiện tại, hãy cho phép đối với trình duyệt hoặc Google Drive đang dùng rồi tiếp tục cài đặt.",
            faq3Q: "Cập nhật app có làm mất dữ liệu không?",
            faq3A: "Không, nếu bạn cài bản mới đè lên cùng ứng dụng KAT Budget. Dữ liệu đang lưu cục bộ sẽ được giữ nguyên. Không gỡ ứng dụng trước khi cập nhật nếu bạn chưa sao lưu dữ liệu.",
            faq4Q: "Đổi máy hoặc sao lưu dữ liệu như thế nào?",
            faq4A: "Trong app, vào Cài đặt > Sao lưu dữ liệu để xuất file .kat và tự lưu file đó ở nơi an toàn. Khi đổi máy hoặc cài lại ứng dụng, cài KAT Budget rồi chọn Cài đặt > Khôi phục dữ liệu để nhập lại file .kat. App không tự tải file sao lưu lên máy chủ.",
            donateKicker: "KAT Budget",
            donateTitle: "Ủng hộ tác giả",
            donateText: "Nếu KAT Budget hữu ích với bạn, phần ủng hộ này giúp tác giả duy trì kiểm thử, lưu trữ và các bản cập nhật sau này.",
            donateBtn: "Thông tin đóng góp",
            modalTitle: "Ủng hộ tác giả",
            modalDesc: "Cảm ơn bạn đã quan tâm và đồng hành cùng KAT Budget.",
            footerText1: `© ${new Date().getFullYear()} KAT GR0UP. Phát triển bởi`,
            footerThanks: "Tác giả chân thành cảm ơn Codex (OpenAI) và Antigravity đã hỗ trợ trong quá trình lên ý tưởng, rà soát nội dung và hoàn thiện KAT Budget.",
            privacyLink: "Chính sách quyền riêng tư",
            backHome: "Trang chủ",
            exitPolicy: "Thoát",
            privacyBadge: "Quyền riêng tư",
            privacyTitle: "Chính sách quyền riêng tư",
            privacyIntro: "KAT Budget không yêu cầu đăng nhập và không thu thập dữ liệu tài chính cá nhân. Dữ liệu như tài khoản, giao dịch, ngân sách, khoản vay và mục tiêu tiết kiệm được lưu cục bộ trong ứng dụng trên thiết bị của bạn.",
            privacyUpdated: "Ngày hiệu lực: 20/05/2026",
            privacySection1Title: "1. KAT Budget không thu thập dữ liệu nào?",
            privacySection1Text: "KAT Budget không yêu cầu bạn tạo tài khoản và không thu thập tên, email, số điện thoại, danh bạ hoặc vị trí. Các dữ liệu tài chính như số dư, giao dịch, ngân sách, khoản vay và mục tiêu tiết kiệm không được gửi về hệ thống của KAT GR0UP.",
            privacySection2Title: "2. Dữ liệu được lưu ở đâu?",
            privacySection2Text: "Dữ liệu của bạn được lưu trong bộ nhớ cục bộ của ứng dụng KAT Budget trên thiết bị. App không đồng bộ dữ liệu tài chính lên máy chủ. Khi cập nhật tỷ giá, app chỉ lấy dữ liệu tham khảo từ Vietcombank và không gửi số dư, tài khoản hoặc lịch sử giao dịch của bạn.",
            privacySection3Title: "3. Sao lưu và chuyển thiết bị",
            privacySection3Text: "Khi cần đổi thiết bị, bạn có thể dùng chức năng Sao lưu dữ liệu trong app để xuất file sao lưu định dạng .kat. File này do bạn tự lưu và tự chuyển sang thiết bị mới để khôi phục dữ liệu. KAT Budget không tự động tải file .kat lên máy chủ; nếu bạn lưu file vào Google Drive, iCloud hoặc dịch vụ đám mây khác, dữ liệu sẽ chịu chính sách của dịch vụ đó.",
            privacySection4Title: "4. Quyền thiết bị",
            privacyPermission1: "Thông báo: dùng để hiển thị các nhắc nhở hoặc cảnh báo do bạn bật trong ứng dụng. KAT Budget không dùng quyền này để đọc dữ liệu cá nhân trên thiết bị.",
            privacyPermission2: "Kết nối mạng: dùng để cập nhật tỷ giá ngoại tệ tham khảo từ Vietcombank. Dữ liệu tài chính cá nhân không được gửi qua yêu cầu này.",
            privacySection5Title: "5. Liên hệ",
            privacyContactText: "Nếu cần hỏi thêm về quyền riêng tư hoặc yêu cầu hỗ trợ, liên hệ qua email:",
            installBadge: "Cài đặt",
            installTitle: "Hướng dẫn cài đặt",
            installIntro: "Bạn có thể cài KAT Budget bằng file APK trên Android. Hoặc sử dụng bản Web App đa nền tảng cho iOS (iPhone/iPad) và Máy tính (Windows/macOS/Linux).",
            installUpdated: "Cập nhật hướng dẫn: 25/05/2026",
            installAndroidTitle: "Android 11+",
            installAndroidText: "Tải file APK từ trang chủ, mở file sau khi tải xong và làm theo hướng dẫn cài đặt. Nếu Android hỏi quyền cài từ nguồn hiện tại, hãy cho phép đối với trình duyệt hoặc Google Drive đang dùng.",
            installIosTitle: "Tất cả phiên bản iOS",
            installIosText: "Mở đường link Web App bằng Safari, chọn 'Chia sẻ' ở dưới cùng màn hình và nhấn 'Thêm vào màn hình chính'. Ứng dụng sẽ hoạt động mượt mà như một app độc lập.",
            installDesktopTitle: "Windows, macOS, Linux",
            installDesktopText: "Truy cập đường link Web App bằng trình duyệt (Chrome, Edge, Safari...) để sử dụng trực tiếp. Không cần cài đặt, dữ liệu vẫn được lưu an toàn trên máy tính của bạn.",
            installNoteTitle: "Lưu ý trước khi cập nhật",
            installNoteText: "Không gỡ app cũ nếu chưa sao lưu. Bạn nên xuất file .kat và giữ ở nơi an toàn; có thể kiểm tra APK qua VirusTotal trước khi cài.",
            installBackButton: "Quay lại trang chủ",
            installDownloadButton: "Tải bản Android"
        },
        en: {
            metaDesc: "KAT Budget helps you track money, plan budgets, and review cash-flow reports. Financial data stays locally on your device.",
            pageTitle: "KAT Budget - Personal Money Management | KAT GR0UP",
            privacyPageTitle: "Privacy Policy - KAT Budget | KAT GR0UP",
            privacyMetaDesc: "KAT Budget does not require sign-in, stores financial data locally, and only uses network access for exchange-rate updates when needed.",
            installPageTitle: "Install Guide - KAT Budget | KAT GR0UP",
            installMetaDesc: "Install guide for KAT Budget on Android and iOS.",
            heroBadge: "Cleaner control for your cash flow",
            heroTitle1: "Understand cash flow.",
            heroTitle2: "Manage smarter.",
            heroDesc: "Track money, plan budgets, and review cash flow in a clean, focused experience.",
            btnDownload: "Download App",
            btnDownloadAndroid: "Download now",
            btnWebApp: "Web App",
            btnInstallGuide: "Install guide",
            btnShare: "Share",
            webAppModalTitle: "Notice & iOS Guide",
            webAppModalDesc: "The cross-platform Web App is still in development and may not be 100% stable.",
            webAppModalGuideTitle: "Install to iOS Home Screen:",
            webAppModalGuide1: "Open the link using the <strong>Safari</strong> browser.",
            webAppModalGuide2: "Tap the <strong>Share</strong> icon at the bottom.",
            webAppModalGuide3: "Select <strong>Add to Home Screen</strong>.",
            webAppModalBtn: "Open Web App Now",
            btnSubtext: "Check APK on VirusTotal",
            btnVersion: "Version 1.0.3",
            downloadVersionLabel: "Version",
            downloadSizeLabel: "Size",
            downloadUpdatedLabel: "Updated",
            downloadPlatformLabel: "Platforms",
            downloadPlatformValue: "Android 11+ · Web App",
            webAppComingSoonText: "Cross-platform Web App is currently in development. Please stay tuned!",
            devBy: "Developed by",
            devSupport: "Support",
            guaranteeTitle: "Data stays locally on your device",
            guaranteeDesc: "KAT Budget does not require an account and does not operate a data-sync server. Balances, transactions, budgets, and loans are stored in the app storage on your device; data only leaves the device when you choose to export, back up, or share a file.",
            releaseKicker: "Version 1.0.3",
            releaseTitle: "What's new?",
            releaseDesc: "This update improves the widget and the multi-currency account setup flow.",
            releaseItem1: "Add multiple opening balances in different currencies when creating an account.",
            releaseItem2: "The widget now shows balance, monthly cash flow, and the latest transaction.",
            releaseItem3: "Quick Income/Expense widget buttons open the right transaction type in the app.",
            archiveKicker: "APK Archive",
            archiveTitle: "Older versions",
            archiveDesc: "We recommend using the latest version for fixes and interface improvements. Older versions are kept for reference or reinstalling only when needed.",
            archiveButton: "Open older versions folder",
            featHeader: "Core Features",
            feat1Title: "Quick Tracking",
            feat1Desc: "Log income, expenses, or transfers with amount, note, date, account, and currency details.",
            feat2Title: "Visual Reports",
            feat2Desc: "Review cash flow by period, currency, and category with readable charts that highlight what matters.",
            feat3Title: "Smart Budgeting",
            feat3Desc: "Track category limits and saving goals so you can adjust spending before exceeding the plan.",
            feat4Title: "Clear Loan Tracking",
            feat4Desc: "Separate lending and borrowing with amount, person, due date, and paid or collected status.",
            feat5Title: "Backup and Data Export",
            feat5Desc: "Create a .kat backup file to restore your data when switching phones or reinstalling the app. For review, you can also export CSV reports by time range, account, or transaction type.",
            ratesKicker: "Automatic rates",
            ratesTitle: "Auto-updating exchange rates",
            ratesDesc: "When online, KAT Budget uses reference rates from Vietcombank to convert values back to VND. Your financial data still stays on your device.",
            ratesBaseLabel: "Conversion base",
            ratesNoteBefore: "Data source:",
            ratesNoteAfter: "Rates are for reference within the app only, not real-time trading values.",
            cpKicker: "Cross-Platform Web App",
            cpTitle: "Experience on any device",
            cpDesc: "KAT Budget is a fully-featured Web App. You can use it smoothly on any browser across macOS, Windows, or Linux without installing anything.",
            previewHeader: "In-App Workflow",
            previewKicker: "Common actions",
            previewTitle: "Visual charts, CSV export when needed",
            previewDesc: "KAT Budget presents cash flow through readable charts and supports CSV export for personal storage or review.",
            previewPoint1: "Cash-flow charts",
            previewPoint2: "Export CSV",
            previewPoint3: "Self-stored data",
            faqHeader: "Frequently Asked Questions",
            faq1Q: "Can I check the APK before installing it?",
            faq1A: "Yes. The APK downloaded from this website includes a",
            faq1B: "link so you can review the scan result before installing. Financial data is stored locally; the app does not require an account and does not operate a data-sync server.",
            faq2Q: "How do I install the APK on Android?",
            faq2A: "Tap the Download App button, open the APK after it finishes downloading, and follow the on-screen steps. If Android asks for permission to install from the current source, allow it for the browser or Google Drive app you are using, then continue the installation.",
            faq3Q: "Will updating the app delete my data?",
            faq3A: "No, as long as you install the new version over the same KAT Budget app. Locally stored data will remain in place. Do not uninstall the app before updating unless you have backed up your data.",
            faq4Q: "How do I switch phones or back up my data?",
            faq4A: "In the app, go to Settings > Backup Data to export a .kat file and keep it somewhere safe. When switching phones or reinstalling the app, install KAT Budget, then choose Settings > Restore Data to import that .kat file. The app does not upload backup files to a server.",
            donateKicker: "KAT Budget",
            donateTitle: "Support the author",
            donateText: "If KAT Budget is useful to you, this support helps the author maintain testing, hosting, and future updates.",
            donateBtn: "Contribution details",
            modalTitle: "Support the author",
            modalDesc: "Thank you for your interest and for supporting KAT Budget.",
            footerText1: `© ${new Date().getFullYear()} KAT GR0UP. Developed by`,
            footerThanks: "The author sincerely thanks Codex (OpenAI) and Antigravity for supporting the ideation, content review, and completion of KAT Budget.",
            privacyLink: "Privacy Policy",
            backHome: "Home",
            exitPolicy: "Close",
            privacyBadge: "Privacy",
            privacyTitle: "Privacy Policy",
            privacyIntro: "KAT Budget does not require sign-in and does not collect personal finance data. Accounts, transactions, budgets, loans, and saving goals are stored locally inside the app on your device.",
            privacyUpdated: "Effective date: May 19, 2026",
            privacySection1Title: "1. What data does KAT Budget not collect?",
            privacySection1Text: "KAT Budget does not require an account and does not collect your name, email, phone number, contacts, or location. Financial data such as balances, transactions, budgets, loans, and saving goals is not sent to KAT GR0UP systems.",
            privacySection2Title: "2. Where data is stored",
            privacySection2Text: "Your data is stored in KAT Budget's local app storage on your device. The app does not sync financial data to a server. When updating exchange rates, it only requests reference rate data from Vietcombank and does not send your balances, accounts, or transaction history.",
            privacySection3Title: "3. Backup and device transfer",
            privacySection3Text: "When switching devices, you can use Backup Data in the app to export a .kat backup file. You keep and transfer this file yourself, then restore it on the new device. KAT Budget does not automatically upload .kat files to a server; if you save the file to Google Drive, iCloud, or another cloud service, that data is covered by that service's policy.",
            privacySection4Title: "4. Device permissions",
            privacyPermission1: "Notifications: used to show reminders or alerts you enable in the app. KAT Budget does not use this permission to read personal data from your device.",
            privacyPermission2: "Network access: used to update reference exchange rates from Vietcombank. Personal financial data is not sent with this request.",
            privacySection5Title: "5. Contact",
            privacyContactText: "For privacy questions or support requests, contact:",
            installBadge: "Installation",
            installTitle: "Install Guide",
            installIntro: "You can install KAT Budget via APK on Android, or use the cross-platform Web App for iOS (iPhone/iPad) and Desktop (Windows/macOS/Linux).",
            installUpdated: "Guide updated: May 25, 2026",
            installAndroidTitle: "Android 11+",
            installAndroidText: "Download the APK from the homepage, open it after the download finishes, and follow the install steps. If Android asks for permission to install from the current source, allow it for the browser or Google Drive app you are using.",
            installIosTitle: "All iOS versions",
            installIosText: "Open the Web App link using Safari, select 'Share' at the bottom of the screen and tap 'Add to Home Screen'. The app will run smoothly like a standalone app.",
            installDesktopTitle: "Windows, macOS, Linux",
            installDesktopText: "Access the Web App link using any browser (Chrome, Edge, Safari...) to use it directly. No installation required, all data is safely stored on your computer.",
            installNoteTitle: "Before updating",
            installNoteText: "Do not uninstall the old app before backing up. Export the .kat file, keep it somewhere safe, and check the APK on VirusTotal before installing if needed.",
            installBackButton: "Back to homepage",
            installDownloadButton: "Download Android version"
        },
        ja: {
            metaDesc: "KAT Budgetは支出の記録、予算の計画、キャッシュフローレポートの確認をサポートします。財務データはデバイスのローカルに保存されます。",
            pageTitle: "KAT Budget - 個人のお金管理 | KAT GR0UP",
            privacyPageTitle: "プライバシーポリシー - KAT Budget | KAT GR0UP",
            privacyMetaDesc: "KAT Budgetはログイン不要で、財務データをローカルに保存し、必要な時だけネットワークを使用して為替レートを更新します。",
            installPageTitle: "インストールガイド - KAT Budget | KAT GR0UP",
            installMetaDesc: "AndroidおよびiOS向けのKAT Budgetインストールガイド。",
            heroBadge: "キャッシュフローをよりスマートに管理",
            heroTitle1: "お金の流れを理解する。",
            heroTitle2: "より賢く管理する。",
            heroDesc: "支出の記録、予算の計画、キャッシュフローの確認を、シンプルで分かりやすい体験で実現します。",
            btnDownload: "アプリをDL",
            btnDownloadAndroid: "ダウンロード",
            btnWebApp: "Webアプリ",
            btnInstallGuide: "インストール",
            btnShare: "共有",
            webAppModalTitle: "注意事項＆iOSガイド",
            webAppModalDesc: "クロスプラットフォームのWebアプリは現在開発中のため、100%完全ではない場合があります。",
            webAppModalGuideTitle: "iOSのホーム画面にインストール：",
            webAppModalGuide1: "<strong>Safari</strong>ブラウザでリンクを開きます。",
            webAppModalGuide2: "下部の<strong>共有</strong>アイコンをタップします。",
            webAppModalGuide3: "<strong>ホーム画面に追加</strong>を選択します。",
            webAppModalBtn: "今すぐWebアプリを開く",
            btnSubtext: "VirusTotalでAPKを確認",
            btnVersion: "バージョン 1.0.3",
            downloadVersionLabel: "バージョン",
            downloadSizeLabel: "サイズ",
            downloadPlatformLabel: "プラットフォーム",
            downloadPlatformValue: "Android 11+ · Webアプリ（クロスプラットフォーム）",
            downloadUpdatedLabel: "更新日",
            webAppComingSoonText: "クロスプラットフォームのWebアプリは現在開発中です。もうしばらくお待ちください！",
            devBy: "開発者",
            devSupport: "サポート",
            guaranteeTitle: "データはデバイスのローカルに保存されます",
            guaranteeDesc: "KAT Budgetはアカウント不要で、データ同期サーバーを運用していません。残高、取引、予算、ローンはデバイス上のアプリのストレージに保存されます。データはエクスポート、バックアップ、共有を自ら行った場合にのみデバイスから離れます。",
            releaseKicker: "バージョン 1.0.3",
            releaseTitle: "新着情報",
            releaseDesc: "このアップデートでは、ウィジェットと複数通貨のアカウント作成フローを改善しました。",
            releaseItem1: "アカウント作成時に、複数通貨の開始残高を追加できます。",
            releaseItem2: "ウィジェットに残高、月間キャッシュフロー、最新の取引を表示します。",
            releaseItem3: "ウィジェットの収入/支出ボタンから、正しい取引タイプをすぐに開けます。",
            archiveKicker: "APKアーカイブ",
            archiveTitle: "古いバージョン",
            archiveDesc: "修正やUIの改善を得るために、最新バージョンを使用することをお勧めします。古いバージョンは参照用、または本当に必要な場合の再インストール用に保持されています。",
            archiveButton: "古いバージョンのフォルダを開く",
            featHeader: "主な機能",
            feat1Title: "素早い記録",
            feat1Desc: "収入、支出、アカウント間の送金を、金額、メモ、日付、アカウント、通貨の詳細とともに記録します。",
            feat2Title: "視覚的なレポート",
            feat2Desc: "期間、通貨、カテゴリー別にキャッシュフローを読みやすいグラフで確認し、重要なポイントを把握できます。",
            feat3Title: "スマートな予算管理",
            feat3Desc: "カテゴリーの制限と貯金目標を追跡し、計画を超える前に支出を調整できます。",
            feat4Title: "明確なローン追跡",
            feat4Desc: "貸し借りを分け、金額、関連する人、期日、回収または返済のステータスを管理します。",
            feat5Title: "バックアップとデータのエクスポート",
            feat5Desc: "スマートフォンの機種変更やアプリの再インストール時にデータを復元できるよう、.katバックアップファイルを作成します。確認用に、期間、アカウント、または取引の種類別にCSVレポートをエクスポートすることもできます。",
            ratesKicker: "自動為替レート",
            ratesTitle: "為替レートの自動更新",
            ratesDesc: "オンライン時、KAT BudgetはVietcombankから参照レートを取得し、VNDに換算します。財務データはデバイスに残ります。",
            ratesBaseLabel: "換算の基準",
            ratesNoteBefore: "データソース：",
            ratesNoteAfter: "レートはアプリ内での参考用であり、リアルタイムの取引価格ではありません。",
            cpKicker: "クロスプラットフォームWebアプリ",
            cpTitle: "すべてのデバイスで体験",
            cpDesc: "KAT Budgetはフル機能のWebアプリです。インストール不要で、macOS、Windows、Linuxのどのブラウザでもスムーズに使用できます。",
            previewHeader: "アプリ内のワークフロー",
            previewKicker: "よく使う操作",
            previewTitle: "視覚的なグラフ、必要に応じてCSVエクスポート",
            previewDesc: "KAT Budgetは読みやすいグラフを通じてキャッシュフローを提示し、個人的な保存や確認のためのCSVエクスポートをサポートします。",
            previewPoint1: "キャッシュフローグラフ",
            previewPoint2: "CSVをエクスポート",
            previewPoint3: "データの自己保存",
            faqHeader: "よくある質問",
            faq1Q: "インストール前にAPKを確認できますか？",
            faq1A: "はい。このウェブサイトからダウンロードしたAPKには、",
            faq1B: "リンクが含まれており、インストール前にスキャン結果を確認できます。財務データはローカルに保存されます。アプリはアカウントを必要とせず、データ同期サーバーも運用していません。",
            faq2Q: "AndroidにAPKをインストールするにはどうすればよいですか？",
            faq2A: "「アプリをダウンロード」ボタンをタップし、ダウンロードが完了したらAPKを開き、画面の指示に従います。現在のソースからインストールする許可をAndroidから求められた場合は、使用しているブラウザまたはGoogle Driveアプリに許可を与え、インストールを続行します。",
            faq3Q: "アプリを更新するとデータは消えますか？",
            faq3A: "いいえ、同じKAT Budgetアプリに新しいバージョンを上書きインストールする限り消えません。ローカルに保存されたデータはそのまま保持されます。データをバックアップしていない限り、更新前にアプリをアンインストールしないでください。",
            faq4Q: "機種変更やデータのバックアップはどうすればよいですか？",
            faq4A: "アプリ内で「設定」>「データのバックアップ」に進み、.katファイルをエクスポートして安全な場所に保管します。機種変更やアプリの再インストール時は、KAT Budgetをインストールし、「設定」>「データの復元」を選択してその.katファイルをインポートします。アプリはバックアップファイルをサーバーにアップロードしません。",
            donateKicker: "KAT Budget",
            donateTitle: "作者をサポート",
            donateText: "KAT Budgetがお役に立った場合、このサポートは作者がテスト、ホスティング、今後の更新を維持するのに役立ちます。",
            donateBtn: "寄付の詳細",
            modalTitle: "作者をサポート",
            modalDesc: "KAT Budgetに関心をお寄せいただき、サポートしていただきありがとうございます。",
            footerText1: `© ${new Date().getFullYear()} KAT GR0UP. 開発：`,
            footerThanks: "作者は、KAT Budgetのアイデア出し、コンテンツのレビュー、完成をサポートしてくれたCodex (OpenAI)とAntigravityに心から感謝します。",
            privacyLink: "プライバシーポリシー",
            backHome: "ホーム",
            exitPolicy: "閉じる",
            privacyBadge: "プライバシー",
            privacyTitle: "プライバシーポリシー",
            privacyIntro: "KAT Budgetはサインインを必要とせず、個人の財務データを収集しません。アカウント、取引、予算、ローン、貯金目標は、デバイス上のアプリ内にローカルに保存されます。",
            privacyUpdated: "発効日：2026年5月25日",
            privacySection1Title: "1. KAT Budgetが収集しないデータは？",
            privacySection1Text: "KAT Budgetはアカウントを必要とせず、名前、メールアドレス、電話番号、連絡先、位置情報を収集しません。残高、取引、予算、ローン、貯金目標などの財務データは、KAT GR0UPのシステムには送信されません。",
            privacySection2Title: "2. データが保存される場所",
            privacySection2Text: "データはデバイス上のKAT Budgetのローカルアプリストレージに保存されます。アプリは財務データをサーバーに同期しません。為替レートを更新する際、Vietcombankに参照レートデータを要求するだけであり、残高、アカウント、取引履歴を送信することはありません。",
            privacySection3Title: "3. バックアップとデバイスの移行",
            privacySection3Text: "デバイスを変更する際、アプリの「データのバックアップ」を使用して.katバックアップファイルをエクスポートできます。このファイルは自分で保持・移行し、新しいデバイスで復元します。KAT Budgetは.katファイルをサーバーに自動的にアップロードしません。ファイルをGoogle Drive、iCloud、または他のクラウドサービスに保存した場合、そのデータは該当サービスのポリシーの対象となります。",
            privacySection4Title: "4. デバイスの権限",
            privacyPermission1: "通知：アプリ内で有効にしたリマインダーやアラートを表示するために使用されます。KAT Budgetは、この権限を使用してデバイスから個人データを読み取ることはありません。",
            privacyPermission2: "ネットワークアクセス：Vietcombankからの参照為替レートを更新するために使用されます。個人の財務データがこのリクエストと共に送信されることはありません。",
            privacySection5Title: "5. 連絡先",
            privacyContactText: "プライバシーに関する質問やサポートのリクエストについては、以下にご連絡ください：",
            installBadge: "インストール",
            installTitle: "インストールガイド",
            installIntro: "KAT BudgetはAndroidのAPK経由でインストールするか、iOS（iPhone/iPad）およびデスクトップ（Windows/macOS/Linux）向けのクロスプラットフォームWebアプリを使用できます。",
            installUpdated: "ガイド更新日：2026年5月25日",
            installAndroidTitle: "Android 11+",
            installAndroidText: "ホームページからAPKをダウンロードし、ダウンロードが完了したら開いて、インストールの手順に従います。現在のソースからインストールする許可をAndroidから求められた場合は、使用しているブラウザまたはGoogle Driveアプリに許可を与えてください。",
            installIosTitle: "すべてのiOSバージョン",
            installIosText: "Safariを使用してWebアプリのリンクを開き、画面下部の「共有」を選択して「ホーム画面に追加」をタップします。アプリはスタンドアロンアプリのようにスムーズに動作します。",
            installDesktopTitle: "Windows、macOS、Linux",
            installDesktopText: "任意のブラウザ（Chrome、Edge、Safariなど）を使用してWebアプリのリンクにアクセスし、直接使用します。インストールは不要で、すべてのデータはコンピューターに安全に保存されます。",
            installNoteTitle: "更新する前に",
            installNoteText: "バックアップする前に古いアプリをアンインストールしないでください。.katファイルをエクスポートして安全な場所に保管し、必要に応じてインストール前にVirusTotalでAPKを確認してください。",
            installBackButton: "ホームページに戻る",
            installDownloadButton: "Android版をダウンロード"
        }

    };

    const langBtn = document.getElementById('lang-btn');
    let currentLang = localStorage.getItem('lang') || 'vi';
    const isPrivacyPage = Boolean(document.querySelector('.policy-main'));
    const isInstallPage = Boolean(document.querySelector('.install-main'));

    const applyLanguage = (lang) => {
        if (!dictionary[lang]) lang = 'vi';
        
        const langMap = { vi: 'VI', en: 'EN', ja: 'JA' };
        if (langBtn) langBtn.textContent = langMap[lang] || 'VI';
        document.documentElement.lang = lang;
        document.querySelectorAll('[data-i18n]').forEach(el => {
            const key = el.getAttribute('data-i18n');
            if (dictionary[lang][key]) el.innerHTML = dictionary[lang][key];
        });
        let pageTitle = dictionary[lang].pageTitle;
        let metaDesc = dictionary[lang].metaDesc;
        if (isPrivacyPage) {
            pageTitle = dictionary[lang].privacyPageTitle;
            metaDesc = dictionary[lang].privacyMetaDesc;
        } else if (isInstallPage) {
            pageTitle = dictionary[lang].installPageTitle;
            metaDesc = dictionary[lang].installMetaDesc;
        }
        const titleEl = document.getElementById('page-title');
        const descEl = document.getElementById('meta-desc');
        const ogTitleEl = document.getElementById('meta-og-title');
        const ogDescEl = document.getElementById('meta-og-desc');
        if (titleEl) titleEl.textContent = pageTitle;
        if (descEl) descEl.setAttribute('content', metaDesc);
        if (ogTitleEl) ogTitleEl.setAttribute('content', pageTitle);
        if (ogDescEl) ogDescEl.setAttribute('content', metaDesc);
    };

    applyLanguage(currentLang);
    if (langBtn) {
        langBtn.addEventListener('click', () => {
            
            const nextLang = { vi: 'en', en: 'ja', ja: 'vi' };
            currentLang = nextLang[currentLang] || 'vi';
            applyLanguage(currentLang);
            localStorage.setItem('lang', currentLang);
            haptic();
            microClick();
        });
    }

    // === 3. XỬ LÝ MODAL ===
    const waModal = document.getElementById('webAppModal');
    const openWaBtn = document.getElementById('webAppBtn');
    const closeWaBtn = document.getElementById('closeWebAppModal');
    const continueWaBtn = document.getElementById('continueToWebAppBtn');
    
    if (waModal && openWaBtn && closeWaBtn) {
        const closeWaModal = () => { waModal.classList.remove('show'); document.body.style.overflow = 'auto'; };
        openWaBtn.addEventListener('click', (e) => { 
            e.preventDefault(); 
            waModal.classList.add('show'); 
            document.body.style.overflow = 'hidden'; 
        });
        closeWaBtn.addEventListener('click', closeWaModal);
        if(continueWaBtn) continueWaBtn.addEventListener('click', closeWaModal);
        window.addEventListener('click', (e) => { if (e.target === waModal) closeWaModal(); });
        document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && waModal.classList.contains('show')) closeWaModal(); });
    }

    const modal = document.getElementById('donateModal');
    const openBtn = document.getElementById('openDonateModal');
    const closeBtn = document.getElementById('closeDonateModal');
    if (modal && openBtn && closeBtn) {
        const closeModal = () => { modal.classList.remove('show'); document.body.style.overflow = 'auto'; };
        openBtn.addEventListener('click', () => { modal.classList.add('show'); document.body.style.overflow = 'hidden'; });
        closeBtn.addEventListener('click', closeModal);
        window.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });
        document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && modal.classList.contains('show')) closeModal(); });
    }

    // === 4. PRELOADER TĨNH ===
    window.addEventListener('load', () => {
        const preloader = document.getElementById('preloader');
        // Trì hoãn nhẹ 300ms sau khi tải xong toàn bộ tài nguyên để ẩn preloader một cách mượt mà
        setTimeout(() => {
            if (preloader) preloader.classList.add('hidden');
        }, 300);
    });

    // === 5. HIỆU ỨNG CUỘN (SCROLL REVEAL) ===
    const revealElements = document.querySelectorAll('.reveal');
    if ('IntersectionObserver' in window) {
        const revealObserver = new IntersectionObserver((entries, observer) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('active');
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.1, rootMargin: "0px 0px -50px 0px" });
        revealElements.forEach(el => revealObserver.observe(el));
    } else {
        revealElements.forEach(el => el.classList.add('active'));
    }

    // === 6. ÁNH SÁNG THEO CHUỘT & PARALLAX NHẸ ===
    const supportsFinePointer = window.matchMedia && window.matchMedia('(pointer: fine)').matches;
    if (supportsFinePointer) {
        window.addEventListener('pointermove', (event) => {
            const x = `${event.clientX}px`;
            const y = `${event.clientY}px`;
            html.style.setProperty('--cursor-x', x);
            html.style.setProperty('--cursor-y', y);
        }, { passive: true });

        document.querySelectorAll('.bento-card, .app-preview').forEach(card => {
            card.addEventListener('pointermove', (event) => {
                const rect = card.getBoundingClientRect();
                card.style.setProperty('--mouse-x', `${event.clientX - rect.left}px`);
                card.style.setProperty('--mouse-y', `${event.clientY - rect.top}px`);
            });
        });

        const hero = document.querySelector('.hero');
        const mockupCarousel = document.querySelector('.mockup-carousel');
        if (hero && mockupCarousel) {
            hero.addEventListener('pointermove', (event) => {
                const rect = hero.getBoundingClientRect();
                const x = (event.clientX - rect.left) / rect.width - 0.5;
                const y = (event.clientY - rect.top) / rect.height - 0.5;
                mockupCarousel.style.transform = `translate3d(${x * -18}px, ${y * -12}px, 0) rotateX(${y * 5}deg) rotateY(${x * -7}deg)`;
            });

            hero.addEventListener('pointerleave', () => {
                mockupCarousel.style.transform = '';
            });
        }
    }

    // === 7. SLIDER MOCKUP (KÉO THẢ & CLICK) ===
    const track = document.getElementById('mockupTrack');
    const indicators = document.querySelectorAll('.indicator');
    
    if (track && indicators.length > 0) {
        track.addEventListener('scroll', () => {
            let index = Math.round(track.scrollLeft / track.clientWidth);
            indicators.forEach((ind, i) => ind.classList.toggle('active', i === index));
        });

        indicators.forEach((ind, i) => {
            ind.addEventListener('click', () => {
                track.scrollTo({ left: i * track.clientWidth, behavior: 'smooth' });
            });
        });

        let isDown = false;
        let startX;
        let scrollLeft;

        track.addEventListener('mousedown', (e) => {
            isDown = true;
            track.classList.add('active-drag');
            startX = e.pageX - track.offsetLeft;
            scrollLeft = track.scrollLeft;
        });

        track.addEventListener('mouseleave', () => {
            isDown = false;
            track.classList.remove('active-drag');
        });

        track.addEventListener('mouseup', () => {
            isDown = false;
            track.classList.remove('active-drag');
        });

        track.addEventListener('mousemove', (e) => {
            if (!isDown) return;
            e.preventDefault();
            const x = e.pageX - track.offsetLeft;
            const walk = (x - startX) * 1.5; 
            track.scrollLeft = scrollLeft - walk;
        });
    }

    // === 8. NÚT BACK TO TOP ===
    const backToTopBtn = document.getElementById('backToTop');
    if (backToTopBtn) {
        window.addEventListener('scroll', () => {
            if (window.scrollY > 400) {
                backToTopBtn.classList.add('show');
            } else {
                backToTopBtn.classList.remove('show');
            }
        });

        backToTopBtn.addEventListener('click', () => {
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        });
    }

    // === 9. NÚT CHIA SẺ ===
    const shareBtn = document.getElementById('shareBtn');
    if (shareBtn) {
        shareBtn.addEventListener('click', async () => {
            haptic(50);
            const shareData = {
                title: 'KAT Budget',
                text: currentLang === 'vi'
                    ? 'Quản lý chi tiêu gọn gàng với KAT Budget — miễn phí, đa nền tảng!'
                    : 'Manage your finances with KAT Budget — free, cross-platform!',
                url: 'https://katbudget.vercel.app'
            };
            if (navigator.share) {
                try { await navigator.share(shareData); } catch(e) {}
            } else {
                try {
                    await navigator.clipboard.writeText(shareData.url);
                    const orig = shareBtn.querySelector('span');
                    if (orig) {
                        const prevText = orig.textContent;
                        orig.textContent = currentLang === 'vi' ? 'Đã sao chép!' : 'Copied!';
                        setTimeout(() => { orig.textContent = prevText; }, 2000);
                    }
                } catch(e) {}
            }
        });
    }
});

