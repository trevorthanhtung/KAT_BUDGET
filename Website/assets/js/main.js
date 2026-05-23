document.addEventListener("DOMContentLoaded", () => {
    document.body.classList.add('js-enabled');
    
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
            installMetaDesc: "Hướng dẫn cài đặt KAT Budget trên Android. Bản iOS 16+ sắp ra mắt và sẽ được cập nhật sau.",
            heroBadge: "Gọn gàng hơn cho dòng tiền của bạn",
            heroTitle1: "Thấu hiểu dòng tiền.",
            heroTitle2: "Quản lý thông minh.",
            heroDesc: "Ghi thu chi, lập ngân sách và xem báo cáo dòng tiền trong một trải nghiệm gọn, rõ và dễ theo dõi.",
            btnDownload: "Tải Ứng Dụng",
            btnDownloadAndroid: "Tải ngay",
            btnWebApp: "Web App",
            btnInstallGuide: "Hướng dẫn cài đặt",
            btnSubtext: "Kiểm tra APK trên VirusTotal",
            btnVersion: "Phiên bản v3.0.0",
            downloadVersionLabel: "Phiên bản",
            downloadSizeLabel: "Dung lượng",
            downloadPlatformLabel: "Nền tảng",
            downloadPlatformValue: "Android 11+ · Web App (Đa nền tảng)",
            downloadUpdatedLabel: "Cập nhật",
            webAppComingSoonText: "Web App đa nền tảng đang được hoàn thiện. Bạn chờ chút nhé!",
            devBy: "Phát triển bởi",
            devSupport: "Hỗ Trợ Nhanh",
            guaranteeTitle: "Dữ liệu lưu cục bộ trên thiết bị",
            guaranteeDesc: "KAT Budget không yêu cầu tài khoản và không vận hành máy chủ đồng bộ dữ liệu. Số dư, giao dịch, ngân sách và khoản vay được lưu trong bộ nhớ ứng dụng trên thiết bị; dữ liệu chỉ rời thiết bị khi bạn chủ động xuất, sao lưu hoặc chia sẻ file.",
            releaseKicker: "Phiên bản 1.0.0",
            releaseTitle: "Cập nhật mới có gì?",
            releaseDesc: "Bản cập nhật này tập trung cải tiến giao diện và sửa những lỗi từ bản beta.",
            releaseItem1: "UI/UX hiện đại hơn.",
            releaseItem2: "Sửa lỗi từ bản thử nghiệm.",
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
            footerText1: "© 2026 KAT GR0UP. Phát triển bởi",
            footerThanks: "Tác giả chân thành cảm ơn Codex (OpenAI) và Google Gemini đã hỗ trợ trong quá trình lên ý tưởng, rà soát nội dung và hoàn thiện website KAT Budget.",
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
            installIntro: "Bạn có thể cài KAT Budget bằng file APK trên Android. Bản iOS 16+ sắp ra mắt và sẽ được cập nhật tại website khi sẵn sàng.",
            installUpdated: "Cập nhật hướng dẫn: 21/05/2026",
            installAndroidTitle: "Android 11+",
            installAndroidText: "Tải file APK từ trang chủ, mở file sau khi tải xong và làm theo hướng dẫn cài đặt. Nếu Android hỏi quyền cài từ nguồn hiện tại, hãy cho phép đối với trình duyệt hoặc Google Drive đang dùng.",
            installIosTitle: "iOS 16+",
            installIosText: "Sắp ra mắt. Nút cài đặt iOS hiện chưa hoạt động; thông tin phát hành sẽ được cập nhật khi có bản sẵn sàng.",
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
            installMetaDesc: "Install guide for KAT Budget on Android. The iOS 16+ version is coming soon and will be updated later.",
            heroBadge: "Cleaner control for your cash flow",
            heroTitle1: "Understand cash flow.",
            heroTitle2: "Manage smarter.",
            heroDesc: "Track money, plan budgets, and review cash flow in a clean, focused experience.",
            btnDownload: "Download App",
            btnDownloadAndroid: "Download now",
            btnWebApp: "Web App",
            btnInstallGuide: "Install guide",
            btnSubtext: "Check APK on VirusTotal",
            btnVersion: "Version 3.0.0",
            downloadVersionLabel: "Version",
            downloadSizeLabel: "Size",
            downloadPlatformLabel: "Platform",
            downloadPlatformValue: "Android 11+ · Web App",
            downloadUpdatedLabel: "Updated",
            webAppComingSoonText: "Cross-platform Web App is currently in development. Please stay tuned!",
            devBy: "Developed by",
            devSupport: "Quick Support",
            guaranteeTitle: "Data stays locally on your device",
            guaranteeDesc: "KAT Budget does not require an account and does not operate a data-sync server. Balances, transactions, budgets, and loans are stored in the app storage on your device; data only leaves the device when you choose to export, back up, or share a file.",
            releaseKicker: "Version 1.0.0",
            releaseTitle: "What's new?",
            releaseDesc: "This update focuses on interface improvements and bug fixes from the beta version.",
            releaseItem1: "More modern UI/UX.",
            releaseItem2: "Fixed bugs from the beta version.",
            archiveKicker: "APK archive",
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
            ratesNoteAfter: "Rates are for app reference only, not real-time trading prices.",
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
            footerText1: "© 2026 KAT GR0UP. Developed by",
            footerThanks: "The author sincerely thanks Codex (OpenAI) and Google Gemini for supporting the ideation, content review, and completion of the KAT Budget website.",
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
            installBadge: "Install",
            installTitle: "Install guide",
            installIntro: "You can install KAT Budget on Android using the APK file. The iOS 16+ version is coming soon and will be updated on this website when ready.",
            installUpdated: "Guide updated: May 21, 2026",
            installAndroidTitle: "Android 11+",
            installAndroidText: "Download the APK from the homepage, open it after the download finishes, and follow the install steps. If Android asks for permission to install from the current source, allow it for the browser or Google Drive app you are using.",
            installIosTitle: "iOS 16+",
            installIosText: "Coming soon. The iOS install button is not active yet; release information will be updated when a build is ready.",
            installNoteTitle: "Before updating",
            installNoteText: "Do not uninstall the old app before backing up. Export the .kat file, keep it somewhere safe, and check the APK on VirusTotal before installing if needed.",
            installBackButton: "Back to homepage",
            installDownloadButton: "Download Android version"
        }
    };

    const langBtn = document.getElementById('lang-btn');
    let currentLang = localStorage.getItem('lang') || 'vi';
    const isPrivacyPage = Boolean(document.querySelector('.policy-main'));
    const isInstallPage = Boolean(document.querySelector('.install-main'));

    const applyLanguage = (lang) => {
        if (!dictionary[lang]) lang = 'vi';
        if (langBtn) langBtn.textContent = lang === 'vi' ? 'EN' : 'VI';
        document.documentElement.lang = lang;
        document.querySelectorAll('[data-i18n]').forEach(el => {
            const key = el.getAttribute('data-i18n');
            if (dictionary[lang][key]) el.textContent = dictionary[lang][key];
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
            currentLang = currentLang === 'vi' ? 'en' : 'vi';
            applyLanguage(currentLang);
            localStorage.setItem('lang', currentLang);
        });
    }

    // Logic cho Toast khi bấm nút (hiện không dùng vì Web App đã có link thật)
    // const webAppBtn = document.getElementById('webAppBtn');
    // const siteToast = document.getElementById('siteToast');
    // let toastTimer;
    // if (webAppBtn && siteToast) {
    //     webAppBtn.addEventListener('click', (e) => {
    //         e.preventDefault();
    //         siteToast.classList.add('show');
    //         window.clearTimeout(toastTimer);
    //         toastTimer = window.setTimeout(() => {
    //             siteToast.classList.remove('show');
    //         }, 3600);
    //     });
    // }

    // === 3. XỬ LÝ MODAL ===
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
});

