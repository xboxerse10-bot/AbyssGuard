# AbyssGuard Full — 26.3 Fabric (ALPHA SOURCE)

A new, independent region-protection mod for AbyssMC. This is a **development foundation, not a production-ready WorldGuard replacement**. The build has **not been verified** on Minecraft 26.3; API adjustments may be necessary.

## Implemented in source
- Cuboid regions by dimension; `/rg pos1`, `/rg pos2`, `/rg define <id>`, `/rg delete <id>`, `/rg info <id>`, `/rg list`
- Owner/member UUID lists: `/rg addmember`, `/rg removemember`, `/rg addowner`, `/rg removeowner` (UUID argument)
- `/rg flag <id> <flag> <allow|deny|none>`; text flags accept a message; `/rg priority <id> <number>`
- Region JSON persistence at `config/abyssguard/regions.json`, atomic file replacement
- Highest-priority region resolution, basic owner/membership checks
- First-pass block-break and block-interact event hooks
- Basic anti-overlap check for region creation

## NOT implemented / NOT verified
- No WorldEdit selection bridge yet (stand at each corner and use `/rg pos1` and `/rg pos2`).
- **Only BLOCK_BREAK and INTERACT are hooked into Fabric events**, and neither has been runtime tested. All other flag names are *configuration placeholders* until each protection event is implemented. **BLOCK_PLACE is not protected.**
- No LuckPerms integration, administrator bypass, global regions, parent inheritance, flags on non-player events, movement entry/exit notifications, PvP, explosions, fluid/fire protection, or mob spawning hooks yet.
- Permission security and overlapping region behavior need a dedicated review before public-server deployment.
- No automated test suite or successful 26.3 build yet.
- Commands accept UUIDs rather than player names for owner/member changes.

## Build
Upload **the contents of this directory** to the root of a GitHub repository and run Actions → Build AbyssGuard 26.3. Requires Java 25, Gradle 9.7.1, Fabric Loom 1.18.2, Fabric API 0.166.2+26.3. The workflow uploads `build/libs/*.jar` only if compilation succeeds.

## Important
Do not replace WorldGuard on your live server with this alpha. Use a separate test server and backups. Compile failures should be fixed using the GitHub Actions log, not by removing protections.
