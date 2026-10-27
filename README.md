# AyX Copy v5.0

New build of Copy Me, rebuilt as AyX Copy (package `ayx.copy`).

## Kya karta hai
- Saved shortcuts: type `.upi` ya `,upi` — dono chalenge, kisi bhi app me
  auto-replace ho jata hai (Accessibility Service).
- Quick Copy/Paste: `..` + space = poora field copy + clear,
  `...` + space = clipboard paste.
- AI text generator (Groq, apni key se):
  - `..ai` = text ko aur better / improve karo
  - `..ea` = English me kar do
  - `..ra` = Roman me kar do
  - Kisi bhi app me text type karo, last me trigger likho — AI poora text badal dega.
  - App ke andar AI Generator screen bhi hai (home screen par spark button).
- Export: sirf JSON, sirf `ayx.json` naam se. Import bhi `ayx.json` padhta hai.

## Groq key kaise dalen
1. console.groq.com se apni API key banao (`gsk_...`).
2. App me Settings > AI triggers (Groq) > key dalo > Save Groq key.
3. Key sirf device par rehti hai, export file me nahi jati.
4. AI use karne par sirf wahi text Groq ko bheja jata hai.

## Build
AndroidIDE (phone) me project kholo aur Build dabao. ActionBar error se bachne
ke liye theme `AppTheme` (Material3 Dark, NoActionBar) `values/` aur
`values-night/` dono me hai. Saare icons local vector drawables hain, koi web icon nahi.
Launcher icon adaptive vector hai (`mipmap-anydpi-v26`); PNG fallback sirf local
zip me hai, is repo snapshot me binary files nahi hain.

## Version
v5.0 (versionCode 6) — AyX Copy

## GitHub se build (Actions)
Repo me `.github/workflows/build.yml` hai. `main` par push hote hi Actions
chalta hai aur 2 artifact deta hai: `AyXCopy-debug` (hamesha installable)
aur `AyXCopy-release` (secret ho to signed, nahi to unsigned).
Artifact download: repo > Actions > latest run > Artifacts.

## Release signature (GitHub Secrets — keystore kabhi repo me nahi)
Repo PUBLIC hai, isliye keystore file ya password kabhi commit mat karna.
Release sign karne ke liye repo > Settings > Secrets and variables > Actions
me ye 4 secrets dalo:
- `ANDROID_KEYSTORE_BASE64` — ayx-copy.p12 ka base64
- `KEYSTORE_PASSWORD`, `KEY_ALIAS` (`ayx`), `KEY_PASSWORD`
Secrets dalne ke baad workflow dobara chalao (Actions > Run workflow).

## Commands: `.` aur `,` ka rule
- Exact prefix pehle: `.8` sirf `.8` wali row, `,8` sirf `,8` wali row.
  Isliye PasteMe ke 17 paired commands (English `.` / Roman `,`) alag-alag
  rehte hain, ek doosre ko overwrite nahi karte.
- Fallback: agar sirf ek version saved hai (jaise sirf `.ii`), to dono
  prefix usi row ko chalayenge.
- Import: `ayx.json` ke saath-saath PasteMe backup (`templates` format)
  bhi seedha import hota hai, duplicate row nahi banti.
