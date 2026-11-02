# مشروع تطبيق «الإسلام» - Android Studio (FCM & WebView)

تطبيق أندرويد متكامل لموقع تطبيق الإسلام: **https://eslam-app-d6c53.web.app**
- **معرّف الحزمة (Package Name):** `com.eslam.app`
- **إصدار التطبيق:** 2.0 (Build 2)
- **المستهدف:** Android 7.0 (API 24) حتى Android 14+ (API 34)

---

## 🌟 الميزات المدمجة في هذا التحديث:

1. **إشعارات Firebase Cloud Messaging (FCM):**
   - تم دمج خدمة `EslamFirebaseMessagingService` بالكامل لمعالجة الإشعارات بالخلفية والمقدمة.
   - إنشاء قناة إشعارات رسمية `eslam_fcm_notifications_channel` بصوت وأولوية عالية (High Importance).
   - توجيه ذكي: عند إرسال رابط في حقل `url` أو `link` ببيانات الإشعار (Data Payload)، ينتقل التطبيق مباشرة للآية أو القارئ أو القسم المطلوب.
   - استخدام ملف الإعدادات `app/google-services.json` المربوط بالمشروع `eslam-app-d6c53`.

2. **طلب إذن الإشعارات لأندرويد 13 فما فوق:**
   - فحص وطلب إذن `POST_NOTIFICATIONS` تلقائيًا عند تشغيل التطبيق على أجهزة Android 13 (API 33) و Android 14 (API 34).

3. **الحفاظ الكامل على جميع ميزات التطبيق والتصميم:**
   - شاشة البداية الإسلامية الكاملة (Splash Screen).
   - ظهور صور القراء ومشايخ القرآن الكريم تلقائيًا بدون الحاجة للضغط عليها.
   - ضبط مدة سورة الفاتحة للشيخ محمد صديق المنشاوي لتكون 51 ثانية بدقة.
   - التنقل التدريجي بزر الرجوع في الهاتف والأزرار الداخلية (Gradual Back Navigation) خطوة بخطوة دون القفز للرئيسية.
   - إزالة الفراغ السفلي عند التمرير (Bottom Gap Fix).
   - دعم كامل لتنزيل الملفات الصوتية، رفع الملفات، وتشغيل الفيديو ملء الشاشة.

---

## 🚀 كيفية فتح وتشغيل المشروع في Android Studio:

1. فك ضغط ملف المشروع `EslamApp-AndroidStudio-FullProject.zip`.
2. افتح برنامج **Android Studio**، ثم اختر **Open** وحدد مجلد المشروع `EleslamApp-Project`.
3. سيبدأ Android Studio بمزامنة ملفات Gradle تلقائيًا وتنزيل مكتبات Firebase المطلوبة.
4. لبناء التطبيق بصيغة APK:
   - من القائمة العلوية: **Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**.
   - ستجد ملف الـ APK الجاهز داخل المجلد: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📲 كيفية تجربة إرسال إشعار تجريبي عبر Firebase Console:

1. افتح [Firebase Console](https://console.firebase.google.com).
2. اختر مشروعك: **eslam-app-d6c53**.
3. من القائمة الجانبية: **Engage** -> **Messaging** (أو **Cloud Messaging**).
4. اضغط على **Create your first campaign** ثم اختر **Firebase Notification messages**.
5. اكتب عنوان الإشعار ونصه (مثلاً: "تذكير بأذكار المساء").
6. في قسم **Target** اختر تطبيق أندرويد `com.eslam.app`.
7. (اختياري) في قسم **Additional options** -> **Custom data**:
   - أضف مفتاح `url` وقيمته: `https://eslam-app-d6c53.web.app` (أو رابط أي صفحة تود فتحها تلقائياً عند النقر).
8. اضغط **Review** ثم **Publish**. سيصل الإشعار إلى الهاتف بصوت وتنبيه فوري!
