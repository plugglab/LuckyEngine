# ✦ LuckyBlock v1.0 — Paper/Spigot 1.21.x

A feature-rich, fully config-driven Lucky Block plugin.

## Features
- **40+ rewards** across Legendary, Good, Neutral, and Bad tiers
- **Luck system** — each player has a -100 → +100 luck score that biases the random roll
- **PlaceholderAPI support** — 5 placeholders for scoreboards/chat
- **In-game GUI** — browse, create, edit and delete rewards without touching any files
- **Craftable** — default recipe: 8 Glass + 1 Sponge (fully configurable shape)
- **Persistent tracking** — only *placed* Lucky Blocks trigger rewards; natural blocks are safe across restarts
- **Reward types**: ITEM, MULTI_ITEM, COMMAND, MOB_SPAWN, EXPLOSION, POTION_EFFECT (multi-effect!), LIGHTNING, MESSAGE, STRUCTURE, CHEST_LOOT, ENCHANT_HELD, FIREWORKS, TRAP, **XP**
- **Direct XP rewards** — gives XP points and levels via the API, no command plugins required
- **Fixed Obsidian Cage** — teleports player inside a 3×5×3 cage with glass windows, then seals it
- **Treasure Vault** structure — spawns a furnished stone brick room with a filled chest
- **Per-reward lore & custom names** on item rewards
- **Live config reload** — `/lb reload`, no restart needed

---

## Building

Requires JDK 21 and Maven 3.8+. PlaceholderAPI is a *soft* dependency (optional).

```bash
mvn package
```

Output: `target/LuckyBlock.jar` → drop into your server's `plugins/` folder.

---

## Commands

| Command | Description | Permission |
|---|---|---|
| `/lb gui` | Open the in-game GUI menu | `luckyblock.use` |
| `/lb give <player> [amt]` | Give Lucky Block items | `luckyblock.admin` |
| `/lb setluck <player> <n>` | Set luck to exact value (-100→100) | `luckyblock.admin` |
| `/lb addluck <player> <n>` | Add/subtract luck | `luckyblock.admin` |
| `/lb getluck [player]` | Check luck value + bar | `luckyblock.use` |
| `/lb list` | List all rewards with tiers/weights | `luckyblock.use` |
| `/lb reload` | Reload config.yml | `luckyblock.admin` |

Aliases: `/luckyblock`, `/lucky`

---

## GUI

Open with `/lb gui`. Three screens:

**Main Menu** — give yourself a Lucky Block, open the reward list, reload config, see your current luck.

**Reward List** — paginated grid of every loaded reward coloured by tier. Left-click to edit, right-click to delete.

**Reward Editor** — create or edit any reward entirely in-game. Click a field to cycle (Tier, Type) or type a value in chat. Hit Save to write directly to `config.yml` and reload — no server restart needed.

---

## PlaceholderAPI Placeholders

Requires PlaceholderAPI installed on the server.

| Placeholder | Example output |
|---|---|
| `%luckyblock_luck%` | `42` |
| `%luckyblock_luck_formatted%` | `§a+42` |
| `%luckyblock_luck_bar%` | `§a■■■■■■■□□□` |
| `%luckyblock_luck_tier%` | `§aLucky` |
| `%luckyblock_total_rewards%` | `43` |

---

## Visual

The Lucky Block uses **Block Display entities** — no resource pack needed:
- **Real block**: Yellow Stained Glass — fully visible and breakable by players with any tool
- **Inner core**: half-scale (0.5×0.5×0.5) Sponge Block Display, centred inside the glass at offset (0.25, 0.25, 0.25)

When broken, the display entity is removed instantly alongside the reward trigger.

---

## Crafting Recipe

Default shape (configurable in `config.yml`):

```
G G G
G S G    →  ✦ Lucky Block
G G G
```

`G` = Glass, `S` = Sponge

Change the shape and ingredients freely in `config.yml` under `crafting:`.

---

## Luck System

Every player starts at luck `0` (neutral). Luck ranges from **-100** (very unlucky) to **+100** (very lucky).

| Luck | Effect on rolls |
|---|---|
| +100 | GREAT rewards get 3× weight; BAD rewards get 0.1× |
| 0 | Neutral — all base weights used as-is |
| -100 | BAD rewards get ~2× weight; GREAT get 0.05× |

Use `/lb setluck <player> 100` to give someone max luck, or hook into other plugins with commands.

---

## Reward Types Reference

| type | Required fields | Notes |
|---|---|---|
| `ITEM` | `material`, `amount` | Optional: `enchantments`, `custom-name`, `lore` |
| `MULTI_ITEM` | `items: []` | Each entry has same fields as ITEM |
| `XP` | `xp-points` and/or `xp-levels` | Gives XP directly via API — no command plugin needed |
| `COMMAND` | `commands: []` | Console-run; `%player%` substituted |
| `MOB_SPAWN` | `mob`, `count` | Optional: `powered`, `tamed`, `with-gear`, `drop-from-sky` |
| `EXPLOSION` | `power`, `break-blocks` | |
| `POTION_EFFECT` | `effects: []` | Each entry: `effect`, `duration-seconds`, `amplifier` |
| `LIGHTNING` | `count`, `damage` | Scattered around the block |
| `MESSAGE` | `message` | Sent only to the player |
| `STRUCTURE` | `structure` | `OBSIDIAN_CAGE`, `COBWEB_TRAP`, `TREASURE_VAULT`, `ORE_VEIN_DIAMOND` |
| `CHEST_LOOT` | `loot-table` | Any `minecraft:chests/...` loot table key |
| `ENCHANT_HELD` | `levels` | Enchants held item with random enchants |
| `FIREWORKS` | `count` | Colourful random fireworks |
| `TRAP` | `trap` | `DROP_HOTBAR` — drops all hotbar items |

### Tier weights

All rewards have a `tier` field: `GREAT`, `GOOD`, `NEUTRAL`, or `BAD`.
At max luck (+100) GREAT rewards are 3× more likely; at -100 they are nearly impossible.

---

## Adding a New Reward (Example)

Via the GUI: `/lb gui` → Reward List → Create New Reward.

Or manually in `config.yml`:

```yaml
  - id: my_custom_sword
    tier: GOOD
    type: ITEM
    weight: 5
    display-name: "&cBlood Sword!"
    material: DIAMOND_SWORD
    amount: 1
    custom-name: "&4Blood Sword"
    lore:
      - "&7Drips with the luck of the fallen"
    enchantments:
      - "SHARPNESS:5"
      - "FIRE_ASPECT:2"
      - "LOOTING:3"
      - "UNBREAKING:3"
```

XP reward example:

```yaml
  - id: big_xp_drop
    tier: GOOD
    type: XP
    weight: 10
    display-name: "&a1000 XP + 3 Levels!"
    xp-points: 1000
    xp-levels: 3
```

Append to the `rewards:` list and run `/lb reload`, or use the in-game editor.
