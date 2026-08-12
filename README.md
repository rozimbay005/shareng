# Wifi Transfer

Ikkita Android telefon o'rtasida **Wi-Fi Direct** orqali fayl/dastur (.apk va h.k.)
uzatuvchi sodda dastur. Router yoki umumiy WiFi tarmog'i shart emas — ikkala
holatda ham ishlaydi, chunki Wi-Fi Direct telefonlarni to'g'ridan-to'g'ri bog'laydi.

## Qanday ishlaydi

1. Ikkala telefonda ham dastur ochiladi, "Qurilmalarni qidirish" bosiladi
2. Bir telefon ro'yxatdan ikkinchisini tanlab, ulanish so'rovi yuboradi
3. Ikkinchi telefonda tizim ulanishni tasdiqlash oynasini chiqaradi (standart
   Android xatti-harakati)
4. Ulanish o'rnatilgach, tizim avtomatik ravishda bitta telefonni "guruh egasi"
   (group owner) qilib belgilaydi — u fayl qabul qiluvchi (server) bo'ladi,
   ikkinchisi esa yuboruvchi (client) bo'ladi
5. Guruh a'zosi (client) tomonda "Fayl yuborish" tugmasi faollashadi, fayl
   tanlansa, avtomatik uzatiladi
6. Qabul qilingan fayllar `Downloads/WifiTransfer/` papkasiga saqlanadi

## Android Studio'da ochish

1. Android Studio'ni oching → **Open** → shu papkani tanlang
2. Gradle sinxronlanishini kuting (birinchi marta internet kerak,
   kutubxonalarni yuklab olish uchun)
3. Ikkita real qurilmani (yoki bittasi real, bittasi emulyator — lekin
   Wi-Fi Direct emulyatorda ishlamaydi, shuning uchun **ikkalasi ham real
   telefon** bo'lishi kerak) USB orqali ulang yoki APK yig'ib o'rnating
4. **Run** tugmasi bilan ishga tushiring

## Muhim eslatmalar

- **Ikkala qurilma ham real telefon bo'lishi shart** — Wi-Fi Direct
  emulyatorlarda ishlamaydi.
- Birinchi ishga tushirishda tizim joylashuv (location) ruxsatini so'raydi —
  bu Google'ning talabi (Wi-Fi P2P skanerlash uchun shart), dastur
  joylashuvni real ishlatmaydi.
- Katta fayllar (masalan .apk, video) uzatishda telefon ekranini
  o'chirmaslikka harakat qiling — fon rejimida uzatish keyingi bosqichda
  qo'shsa bo'ladigan funksiya.
- Hozirgi versiyada bitta faylni tanlab, birma-bir yuborasiz. Bir nechta
  faylni birdaniga yuborish keyingi qadam sifatida qo'shiladi.

## GitHub orqali APK yig'ish (Android Studio shart emas)

Bu loyihada `.github/workflows/build.yml` fayli bor — GitHub'ga push qilganingizda
u avtomatik ravishda APK'ni yig'ib beradi.

1. GitHub'da yangi repository yarating (masalan `wifi-transfer`)
2. Shu papkani o'sha repoga yuklang:
   ```bash
   cd wifi-transfer
   git init
   git add .
   git commit -m "Wifi Transfer - dastlabki versiya"
   git branch -M main
   git remote add origin https://github.com/FOYDALANUVCHI_NOMI/wifi-transfer.git
   git push -u origin main
   ```
3. GitHub sahifasida repo ichida **Actions** bo'limiga o'ting — build avtomatik
   boshlanadi (yashil ✅ belgisi chiqguncha 2-4 daqiqa kutish kerak bo'lishi mumkin)
4. Build tugagach, o'sha ishlagan workflow'ni ochib, pastda **Artifacts**
   bo'limidan `wifi-transfer-debug-apk` faylini yuklab oling — bu ZIP ichida
   tayyor `.apk` bor
5. APK'ni telefoningizga (yoki ikkalasiga) ko'chirib, o'rnating (noma'lum
   manbalardan o'rnatishga ruxsat berish kerak bo'lishi mumkin)

**Eslatma:** bu debug (test) versiya, imzolanmagan (unsigned) release emas —
shaxsiy foydalanish uchun to'liq yetarli, lekin do'kon (Play Store)ga chiqarish
uchun alohida imzolash jarayoni kerak bo'ladi.

## Keyingi qadamlar (xohlasangiz)

- Bir nechta faylni birdaniga tanlab yuborish
- Progress bar (foiz ko'rsatuvchi chiziq) qo'shish
- Fon xizmati (foreground service) orqali uzatish, ekran o'chsa ham davom etishi
- Bir xil WiFi tarmog'ida (router orqali) ham ishlaydigan NSD-based rejim
  qo'shish — bu joriy transfer kodini o'zgartirmaydi, faqat qurilma
  topish usulini almashtiradi
