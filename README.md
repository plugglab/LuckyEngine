# ✦ LuckyBlock v1.0

> A feature-rich Lucky Block plugin for **Paper / Spigot 1.21.x** with a full in-game GUI, a luck system, multi-language support, PlaceholderAPI integration, and 40+ configurable rewards.

---

## Quick Start

```bash
mvn package          # requires JDK 21 + Maven 3.8+
```

Drop `target/LuckyBlock.jar` into your server's `plugins/` folder and restart.
Give yourself a Lucky Block with `/lb give <yourname>`, place it, break it.

---

## Requirements

| Requirement | Version |
|---|---|
| Java | 21+ |
| Paper / Spigot | 1.21.x |
| PlaceholderAPI | Optional — auto-detected |

---

## Installation

1. Build or download the jar
2. Drop into `plugins/`
3. Restart the server
4. Edit `plugins/LuckyBlock/config.yml` to your liking
5. Edit `plugins/LuckyBlock/lang/en_US.yml` (or `pl_PL.yml`) to customise all messages
6. Run `/lb reload` — no restart needed for config/lang changes

---

## Commands

| Command | Description | Permission |
|---|---|---|
| `/lb gui` | Open the main GUI menu | `luckyblock.use` |
| `/lb give <player> [amt]` | Give Lucky Block items | `luckyblock.admin` |
| `/lb setluck <player> <n>` | Set luck to exact value (−100 → +100) | `luckyblock.admin` |
| `/lb addluck <player> <n>` | Add or subtract luck | `luckyblock.admin` |
| `/lb getluck [player]` | Check luck value and bar | `luckyblock.use` |
| `/lb list` | List all rewards with tiers and weights | `luckyblock.use` |
| `/lb reload` | Reload config.yml and lang files | `luckyblock.admin` |

Aliases: `/luckyblock`, `/lucky`

---

## Permissions

| Node | Default | Description |
|---|---|---|
| `luckyblock.use` | Everyone | Basic commands and GUI browsing |
| `luckyblock.admin` | OP | Give blocks, edit rewards, manage luck, reload |

---

## The Lucky Block Visual

No resource pack needed. Uses vanilla **Block Display entities**:

- **Real block**: Yellow Stained Glass — solid, fully breakable by players with any tool
- **Inner core**: Sponge Block Display at 0.5× scale, centred inside the glass at offset (0.25, 0.25, 0.25)

When broken, the display entity is removed instantly and the reward fires.

---

## Crafting Recipe

Default (configurable in `config.yml` under `crafting:`):

```
G G G
G S G   →   ✦ Lucky Block
G G G
```

`G` = Glass · `S` = Sponge

---

## Luck System

Every player starts at luck `0`. Range: **−100** (very unlucky) → **+100** (very lucky).

| Luck | Effect |
|---|---|
| +100 | GREAT rewards get 3× weight; BAD rewards shrink to 0.05× |
| 0 | All base weights apply as configured |
| −100 | BAD rewards get ~2× weight; GREAT rewards near impossible |

Players receive a luck HUD in chat every time they break a block (toggleable in config).

---

## In-Game GUI

Open with `/lb gui`. Three main screens:

### Main Menu
Shows your current luck bar. Buttons: Browse Rewards, Create New Reward, Give yourself a Lucky Block, Reload Config.

### Reward List
Paginated grid of every loaded reward. Left-click to edit, right-click to instantly delete.

### Reward Editor
All reward fields visible at once. Clicking a field either cycles it (Tier) or opens a dedicated sub-GUI:

| Field | Sub-GUI | Description |
|---|---|---|
| Type | **Type Selector** | Icon grid of all 14 reward types with descriptions |
| Enchantments | **Enchantment Editor** | Full browser with level +/− per enchant |
| Mob | **Mob Selector** | 36-mob grid + powered/tamed/gear/sky toggles + count |
| Potion Effect | **Potion Editor** | All 33 effects, click to add/edit duration+amp |
| Commands | **Command Editor** | 18 command slots, 8 placeholder buttons, 12 templates |

Save writes directly to `config.yml` and reloads — no restart.

---

## Reward Types

| Type | Key fields | Notes |
|---|---|---|
| `ITEM` | `material`, `amount` | Optional: `enchantments`, `custom-name`, `lore` |
| `MULTI_ITEM` | `items: []` | Each entry has same fields as ITEM |
| `XP` | `xp-points`, `xp-levels` | Direct API — no Essentials conflict |
| `COMMAND` | `commands: []` | Console; `%player%` replaced with player name |
| `MOB_SPAWN` | `mob`, `count` | Flags: `powered`, `tamed`, `with-gear`, `drop-from-sky` |
| `EXPLOSION` | `power`, `break-blocks` | |
| `POTION_EFFECT` | `effects: []` | Each: `effect`, `duration-seconds`, `amplifier` |
| `LIGHTNING` | `count`, `damage` | Scattered; `damage: false` = visual only |
| `MESSAGE` | `message` | Private to the player only |
| `STRUCTURE` | `structure` | `OBSIDIAN_CAGE`, `COBWEB_TRAP`, `TREASURE_VAULT`, `ORE_VEIN_DIAMOND` |
| `CHEST_LOOT` | `loot-table` | Any `minecraft:chests/...` key |
| `ENCHANT_HELD` | `levels` | Randomly enchants the player's held item |
| `FIREWORKS` | `count` | Colourful random fireworks |
| `TRAP` | `trap` | `DROP_HOTBAR` — scatters hotbar on the ground |

---

## Config Reference

```yaml
language: en_US                  # en_US or pl_PL
lucky-block-material: YELLOW_STAINED_GLASS
suppress-normal-drop: true

# Luck
default-luck: 0
luck-min: -100
luck-max: 100
luck-drift-after-break: 0        # change luck value after each break
luck-reset-on-death: false

# Cooldowns
cooldown-seconds: 0              # 0 = no cooldown
cooldown-message: "..."

# Restrictions
allowed-worlds: []               # empty = all worlds
required-tool: ANY               # e.g. GOLDEN_PICKAXE

# Effects
effects:
  break-particle: TOTEM_OF_UNDYING
  break-sound: ENTITY_PLAYER_LEVELUP
  sound-volume: 1.0
  sound-pitch: 1.0
  particle-count: 60

# HUD
hud:
  enabled: true                  # show luck/tier box in chat on break

# Broadcast
broadcast-rewards: true          # only GREAT + GOOD tiers broadcast

# Crafting
crafting:
  enabled: true
  shape: ["GGG","GSG","GGG"]
  ingredients: { G: GLASS, S: SPONGE }
  result-amount: 1
```

---

## Language / Localisation

All player-facing text lives in `plugins/LuckyBlock/lang/<locale>.yml`.

Set `language: en_US` or `language: pl_PL` in `config.yml`.

Files are copied from the jar on first start and can be edited freely. Changes apply on `/lb reload`.

**Bundled languages:**
- `en_US` — English
- `pl_PL` — Polish

To add a new language, copy `en_US.yml`, rename it (e.g. `de_DE.yml`), translate all values, and set `language: de_DE` in config.

---

## PlaceholderAPI

See [`PLACEHOLDERS.md`](PLACEHOLDERS.md) for the full placeholder reference, colour coding, and scoreboard/chat examples.

---

## Adding a Reward via Config

```yaml
rewards:
  - id: my_god_sword
    tier: GREAT
    type: ITEM
    weight: 2
    display-name: "&5God Sword!"
    material: NETHERITE_SWORD
    amount: 1
    custom-name: "&5✦ Sword of Fortune"
    lore:
      - "&7Blessed by luck itself"
    enchantments:
      - "SHARPNESS:6"
      - "FIRE_ASPECT:3"
      - "UNBREAKING:5"
      - "MENDING:1"
```

Or use `/lb gui` → Reward List → Create New Reward for a fully in-game experience.

---

## File Structure

```
plugins/LuckyBlock/
├── config.yml           ← main configuration
├── luck.yml             ← per-player luck values (auto-generated)
├── lucky_blocks.yml     ← placed lucky block locations (auto-generated)
└── lang/
    ├── en_US.yml        ← English messages
    └── pl_PL.yml        ← Polish messages
```

---

## See Also

- [`CHANGELOG.md`](CHANGELOG.md) — full version history
- [`PLACEHOLDERS.md`](PLACEHOLDERS.md) — PlaceholderAPI reference
