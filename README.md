# Psiromouna SMP

Paper 1.21.11 economy plugin for the school SMP. Java 21 required.

## Features in 0.1.0
- Two currencies: **Lefta** and **Shards**, saved in SQLite.
- `/worth`, `/worth search <text>`, `/worth hand` and category GUI.
- `/sell hand` and `/sell all` pay Lefta.
- `/shop` sells configured basic items for Lefta.
- Shard shop includes mob spawners (25 Shards each).
- `/ah`, `/ah sell <price>`, `/ah buy <id>`, `/ah cancel <id>`.
- `/balance` shows both currencies.
- `/currency give <player> <lefta|shards> <amount>` (operator permission).
- Tab list header/footer and live sidebar scoreboard.

## Build
Run `gradle build`. Install `build/libs/PsiromounaSMP-0.1.0.jar` in Paper's `plugins` folder.

## Notes
Prices are configurable in `plugins/PsiromounaSMP/config.yml`. This initial version uses a simple fixed shop and command-based auction listings. PlaceholderAPI is optional and not required for the displays.
