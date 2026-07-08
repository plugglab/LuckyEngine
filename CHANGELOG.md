# Changelog — LuckyBlock

## v1.0.0 — Initial Release

### Core Features
- Lucky Block system using Yellow Stained Glass as the real block with a half-scale Sponge `BlockDisplay` entity centred inside
- Block placement tracking persisted in `lucky_blocks.yml` — only placed Lucky Blocks trigger rewards, naturally-generated glass is untouched
- Weighted random reward selection with four tiers: **GREAT**, **GOOD**, **NEUTRAL**, **BAD**

### Luck System
- Per-player luck value (−100 → +100) stored in `luck.yml`
- Luck biases the weighted random roll — positive luck amplifies GREAT/GOOD weights, negative luck amplifies BAD weights
- Configurable `default-luck`, `luck-min`, `luck-max`, `luck-drift-after-break`, `luck-reset-on-death`

### Reward Types
| Type | Description |
|---|---|
| `ITEM` | Drop one item with optional enchantments, custom name, lore |
| `MULTI_ITEM` | Drop several different items at once |
| `XP` | Give XP points/levels directly via API (no Essentials conflict) |
| `COMMAND` | Run console commands (`%player%` supported) |
| `MOB_SPAWN` | Spawn mobs with options: powered, tamed, with-gear, drop-from-sky |
| `EXPLOSION` | Configurable power + break-blocks toggle |
| `POTION_EFFECT` | Multiple simultaneous effects with per-effect duration and amplifier |
| `LIGHTNING` | Multiple lightning strikes scattered around the block |
| `MESSAGE` | Private chat message to the player |
| `STRUCTURE` | `OBSIDIAN_CAGE`, `COBWEB_TRAP`, `TREASURE_VAULT`, `ORE_VEIN_DIAMOND` |
| `CHEST_LOOT` | Drops from any vanilla loot table |
| `ENCHANT_HELD` | Randomly enchants the held item |
| `FIREWORKS` | Launches colourful random fireworks |
| `TRAP` | `DROP_HOTBAR` — scatters hotbar items on the ground |

### Crafting
- Fully configurable 3×3 shaped recipe (default: 8 Glass + 1 Sponge)
- Live recipe re-registration on `/lb reload`

### In-Game GUI (`/lb gui`)
- **Main Menu** — luck bar, browse, create, give, reload
- **Reward List** — paginated grid, left-click to edit, right-click to delete
- **Reward Editor** — all fields visible at once; sub-GUIs for complex fields:
  - **Type Selector** — icon grid of all 14 reward types
  - **Enchantment Editor** — full enchantment browser, level +/− per enchant, applied-list with remove
  - **Mob Selector** — 36-mob icon grid + toggle buttons (powered, tamed, gear, sky) + count adjuster
  - **Potion Effect Editor** — all 33 effect types, click to add/edit duration+amp, right-click to remove
  - **Command Editor** — up to 18 command slots, 8 placeholder buttons, 12 template commands
- GUI navigation uses a transitioning flag to prevent `InventoryCloseEvent` clearing screen state mid-switch

### Localisation
- Full multi-language support via `plugins/LuckyBlock/lang/<locale>.yml`
- Bundled: **en_US** (English) and **pl_PL** (Polish)
- Every player-facing string is in the lang file — prefix, HUD lines, tier names, GUI titles/buttons, command feedback, item names
- Set `language: en_US` (or `pl_PL`) in `config.yml`

### PlaceholderAPI Integration (optional soft-depend)
| Placeholder | Returns |
|---|---|
| `%luckyblock_luck%` | Raw luck number |
| `%luckyblock_luck_formatted%` | Coloured luck string |
| `%luckyblock_luck_bar%` | 10-segment `■` bar |
| `%luckyblock_luck_tier%` | Tier name string |
| `%luckyblock_total_rewards%` | Number of loaded rewards |

### Commands
| Command | Permission |
|---|---|
| `/lb gui` | `luckyblock.use` |
| `/lb give <player> [amt]` | `luckyblock.admin` |
| `/lb setluck <player> <n>` | `luckyblock.admin` |
| `/lb addluck <player> <n>` | `luckyblock.admin` |
| `/lb getluck [player]` | `luckyblock.use` |
| `/lb list` | `luckyblock.use` |
| `/lb reload` | `luckyblock.admin` |

### Config Additions
- `language` — select locale
- `luck-min` / `luck-max` — clamp luck range
- `luck-reset-on-death` — reset luck on player death
- `cooldown-seconds` — per-player break cooldown
- `cooldown-message` — customisable cooldown message
- `allowed-worlds` — whitelist worlds (empty = all worlds)
- `required-tool` — restrict breaking to a specific tool
- `effects.sound-volume` / `effects.sound-pitch` — fine-tune break sound
- `effects.particle-count` — control particle density
- `hud.enabled` — toggle the in-chat HUD box
