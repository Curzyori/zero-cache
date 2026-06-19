# ZeroCache Design Spec

**Aesthetic Goal:** Notion-Clean Editorial (Reference: ~/Documents/DATA +/design/ALL/notion.md)
**Palette:**
- Primary: #5645D4 (Notion Purple)
- Canvas: #FFFFFF (Clean white)
- Surface: #F6F5F4 (Warm off-white)
- Surface Dark: #181715 (Editorial navy/black)
- Ink: #1A1A1A (Deep ink)
- Muted: #5D5B54 (Slate)
- Hairline: #E5E3DF

**Typography:**
- Display: Serif family (Copernicus/Tiempos-style, but using system serif fallback)
- UI: Inter / system sans-serif
- Hierarchy: 48/36/28/22/18/16/14/13/12 px

**UI Components:**
- Single dashboard, no tabs/segments
- Top bar: app title + flag toggle (no nav drawer)
- Hero card (primary): total cache summary with stats below
- Mode chip (No-Root/Root) inside surface card with permission rows
- Lazy list of app rows: app icon + name + size + per-item clear
- Floating primary button: "Hapus Semua Cache" / "Clear All Cache"

**Interactions:**
- Tap "Hapus Semua Cache" → confirm dialog → progress overlay → completion
- Tap "Root/No-Root" chip → toggle strategy (no rebuild needed)
- Tap permission row → open corresponding system settings
- Tap flag icon → switch language ID ↔ EN (in-app, persists)

**i18n:**
- Default: Bahasa Indonesia (values/strings.xml)
- English override: values-en/strings.xml
- In-app switch via LocaleManager (not system locale dependent)
