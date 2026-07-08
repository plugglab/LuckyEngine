# LuckyBlock — PlaceholderAPI Placeholders

Requires [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/).
LuckyBlock registers automatically when PAPI is present — no `/papi ecloud download` needed.

---

## Luck Placeholders

| Placeholder | Type | Example | Description |
|---|---|---|---|
| `%luckyblock_luck%` | Integer | `42` | Raw luck value (−100 to +100) |
| `%luckyblock_luck_formatted%` | String | `§a+42` | Coloured luck number |
| `%luckyblock_luck_bar%` | String | `§a■■■■■□□□□□` | 10-segment visual bar |
| `%luckyblock_luck_tier%` | String | `§aLucky` | Human-readable tier label |

---

## Break Count — Personal

Counts Lucky Blocks broken by **this specific player** within the given window.

| Placeholder | Description |
|---|---|
| `%luckyblock_breaks_total%` | All-time total (never resets) |
| `%luckyblock_breaks_hourly%` | Last 60 minutes |
| `%luckyblock_breaks_daily%` | Last 24 hours |
| `%luckyblock_breaks_weekly%` | Last 7 days |
| `%luckyblock_breaks_monthly%` | Last 30 days |

---

## Break Count — Global

Counts Lucky Blocks broken by **all players combined** within the given window.

| Placeholder | Description |
|---|---|
| `%luckyblock_breaks_global_total%` | All-time server total |
| `%luckyblock_breaks_global_hourly%` | All players — last 60 minutes |
| `%luckyblock_breaks_global_daily%` | All players — last 24 hours |
| `%luckyblock_breaks_global_weekly%` | All players — last 7 days |
| `%luckyblock_breaks_global_monthly%` | All players — last 30 days |

---

## Misc

| Placeholder | Description |
|---|---|
| `%luckyblock_total_rewards%` | Number of currently loaded rewards |

---

## Implementation Notes

- Break timestamps are stored in `plugins/LuckyBlock/stats.yml`
- Entries older than **31 days** are automatically pruned to keep the file small
- All-time totals (`breaks_total`, `breaks_global_total`) are stored separately and never pruned — they always reflect the true cumulative count
- Counts are recalculated on each placeholder request by filtering the timestamp list — no scheduled resets needed

---

## Luck Colour Coding

| Range | Colour |
|---|---|
| +51 to +100 | `§a` Bold Green |
| +11 to +50 | `§a` Green |
| −10 to +10 | `§e` Yellow |
| −50 to −11 | `§c` Red |
| −100 to −51 | `§4` Bold Dark Red |

## Luck Tier Labels (en_US)

| Range | Label |
|---|---|
| +75 to +100 | Very Lucky |
| +25 to +74 | Lucky |
| −24 to +24 | Neutral |
| −74 to −25 | Unlucky |
| −100 to −75 | Very Unlucky |

---

## Scoreboard Example

```yaml
lines:
  - "&6&lLuckyBlock"
  - "&7Luck: %luckyblock_luck_formatted%"
  - "%luckyblock_luck_bar%"
  - "&7Tier: %luckyblock_luck_tier%"
  - ""
  - "&7Broken today: &e%luckyblock_breaks_daily%"
  - "&7Broken total: &e%luckyblock_breaks_total%"
  - ""
  - "&8Server today: &7%luckyblock_breaks_global_daily%"
```

## Chat Example

```
{DISPLAYNAME} &8[Luck: %luckyblock_luck_formatted%&8]: {MESSAGE}
```

## Leaderboard (DeluxeMenus)

```yaml
# Show a "top breaker" button if player broke 10+ blocks this week
view_requirement:
  requirements:
    active_breaker:
      type: '>='
      input: '%luckyblock_breaks_weekly%'
      output: '10'
```
