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
