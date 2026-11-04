# Design system

This design system applies to every FenuaSmash interface: the web administration, the public display, the TV screen and the umpire app. It gives the product a shared visual identity rooted in French Polynesia.

## Principles

- **One color, one role.** Colors carry meaning (free, in progress, live), not decoration.
- **Readable in real conditions.** Sports hall lighting, a screen several meters away, a tablet held in one hand: strong contrast and stable numbers.
- **Works offline.** Fonts and icons are bundled in the apps; nothing is loaded from the internet.

## Colors

The palette is inspired by the landscapes of Tahiti. Each color has a scale from 50 (lightest) to 950 (darkest).

| Color | Inspiration | Role |
|---|---|---|
| **Lagoon** (`lagon`) | The lagoon | Primary color: buttons, links, free court |
| **Ocean** (`ocean`) | The ocean at night | Dark backgrounds: sidebar, TV screen |
| **Sunset** (`sunset`) | The sunset | Match in progress, scores |
| **Hibiscus** (`hibiscus`) | Tiare and hibiscus flowers | Live, badges, highlights |
| **Sand** (`sable`) | The sand | Warm neutrals: backgrounds, borders, text |
| Red | — | Errors only (standard Tailwind red) |

The token names in parentheses are the ones used in the code (`bg-lagon-600`, `text-sable-600`…).

### Values

| Shade | Lagoon | Ocean | Sunset | Hibiscus | Sand |
|---|---|---|---|---|---|
| 50 | `#ECFBFA` | `#EEF4FB` | `#FFF4ED` | `#FDF2F6` | `#FAF8F5` |
| 100 | `#CFF4F1` | `#D9E6F5` | `#FFE5D3` | `#FCE7EF` | `#F3EFE8` |
| 200 | `#A0E8E3` | `#B6CDEB` | `#FFC7A6` | `#FBCFE0` | `#E7E0D4` |
| 300 | `#64D5D0` | `#86ABDC` | `#FFA06E` | `#F8A8C5` | `#D3C8B6` |
| 400 | `#2BBCBA` | `#5684C8` | `#FB7A3F` | `#F2729F` | `#B3A58E` |
| 500 | `#0FA3A3` | `#3767B3` | `#F25C1F` | `#E8467C` | `#958670` |
| 600 | `#0B8287` | `#2A5197` | `#DE4314` | `#D6285C` | `#786B58` |
| 700 | `#0E676D` | `#23417A` | `#B83213` | `#B91B48` | `#615648` |
| 800 | `#115358` | `#1E3461` | `#922A17` | `#991A3E` | `#4A4238` |
| 900 | `#12454A` | `#142746` | `#762516` | `#811A38` | `#2F2A24` |
| 950 | `#052A2F` | `#0B1A30` | `#401009` | `#4F081C` | `#1A1714` |

On the web, the source of truth is the `@theme` block in `apps/web/src/index.css`.

### Common uses

| Element | Colors |
|---|---|
| Page background | Sand 50 |
| Primary / secondary text | Sand 900 / Sand 600 |
| Borders | Sand 200 to 300 |
| Primary button | Lagoon 600 background, Lagoon 700 on hover, white text |
| Link | Lagoon 700 |
| Sidebar, TV screen | Ocean 950 background, Ocean 200 links, Lagoon 600 active item |
| Free court | Lagoon (badge: Lagoon 100 background, Lagoon 800 text) |
| Match in progress, score | Sunset (text in Sunset 700) |
| Live | Hibiscus (badge: Hibiscus 100 background, Hibiscus 800 text) |
| Disabled court | Sand (badge: Sand 200 background, Sand 700 text) |
| Error | Red 100 background, Red 800 text |

### Contrast rules

- White text only on **Lagoon 600** or darker, **Ocean 500** or darker.
- Colored text on a light background: shade **700 or darker** (Sunset 500 is too light for text).
- On a light tinted background (badge), text uses the **800** shade of the same color.

## Typography

| Use | Font | Why |
|---|---|---|
| Headings | **Outfit** | Rounded and modern, matching the sport and the Polynesian identity |
| Body, interface, scores | **Inter** | Highly readable at small sizes; fixed-width figures |

- Scores and any number that changes live use **tabular figures** (`tabular-nums` in CSS), so the display doesn't shift when a score goes from 9 to 10.
- Two weights are enough: regular (400) and semibold (600).
- Web: fonts installed with Fontsource (`@fontsource-variable/outfit`, `@fontsource-variable/inter`).
- Mobile: fonts bundled as app assets (`assets/fonts`) rather than loaded from Google Fonts, so they work offline.

## Icons

Every interface uses **Hugeicons**, free version, **Stroke Rounded** style.

- Chosen because it includes several badminton icons and is available for both React and Flutter: the administration and the tablet share the same pictograms.
- Stroke width: 1.75. Sizes: 16 px inline with text, 20 px by default, 24 px for primary actions.
- Color: inherits the surrounding text color.
- Accessibility: an icon next to text is decorative (hidden from screen readers); an icon alone in a button has a translated label.

| Platform | Package | Usage |
|---|---|---|
| Web | `@hugeicons/react`, `@hugeicons/core-free-icons` | `<Icon icon={BadmintonIcon} />` (component `apps/web/src/components/ui/Icon.tsx`) |
| Mobile | `hugeicons` | `HugeIcon(icon: HugeIcons.strokeRounded…)` |

## Credits

- Icons: [Hugeicons](https://hugeicons.com), free version.
- Fonts: [Outfit](https://fonts.google.com/specimen/Outfit) and [Inter](https://fonts.google.com/specimen/Inter), under the SIL Open Font License.
