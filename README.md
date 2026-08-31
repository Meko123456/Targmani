# Targmani 🗣

[![CI](https://github.com/Meko123456/Targmani/actions/workflows/ci.yml/badge.svg)](https://github.com/Meko123456/Targmani/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**თარგმანი** (*targmani* — Georgian for "translation") — a small, **offline** translator for the
three languages I actually live in: **Georgian ↔ English ↔ Arabic**. Every translation runs
**on-device** with Google's ML Kit; the network is used only to download each language model
once, and never for the translations themselves.

No accounts, no server, no analytics. After the one-time model download, it works on a plane.

## Why this app exists

Generic translators are online-first, ad-heavy, and rarely handle Georgian well. I move between
Tbilisi (Georgian), an English working life, and Arabic around Dubai — and I want a translator
that: works with no signal, keeps my text on the device, and treats Georgian as a first-class
language, not an afterthought. ML Kit has on-device models for all three, so this is buildable
as a genuinely offline tool.

## Features (planned — see the [issues](https://github.com/Meko123456/Targmani/issues))

- 🔤 **Text translation** — type in one language, get the other, on-device. Georgian, English,
  and Arabic, any pairing.
- 🔁 **One-tap language swap** and a clear source/target picker.
- 📥 **Model manager** — download / delete each language model; see what's ready offline. Wi-Fi-only
  download option so it never eats mobile data.
- 🧠 **Auto-detect source** (ML Kit language identification) so you can paste and go.
- 📷 **Camera translate** — point at a sign or menu; on-device text recognition → translation
  overlaid (a later milestone).
- 🕘 **History & favourites** — recent translations, starred phrases, all on-device.
- 🅰️ **Right-to-left aware** — Arabic input and output lay out correctly.
- 📋 **Copy / share / speak** the result.
- 🔒 **Private & offline** — text never leaves the device; only model files are downloaded.
- 🎨 **Material 3** — dynamic color, light/dark, edge-to-edge.

## Architecture

```
domain/   pure Kotlin, unit-tested — Language (BCP-47 + endonyms + RTL), LanguageCatalog
          (offered languages, direction & model-pair math), TranslationDirection (+ swap),
          Translator (a port over the engine, so view models test against a fake)
data/     DataStore preferences: last direction, model-download settings
translate/ ML Kit-backed Translator implementation (model download + translate)
ui/       Compose — translate screen, language pickers, model manager, history
```

The `domain/` layer never imports ML Kit — it holds only plain Kotlin (language codes,
direction math, the `Translator` interface), so the logic is fully unit-testable and the
ML Kit dependency lives behind one seam.

## Building

```
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Kotlin 2.3, AGP 9, Compose BOM 2026.06, minSdk 26, ML Kit Translate 17.

## License

[MIT](LICENSE) © 2026 Merab Kochlamazashvili
