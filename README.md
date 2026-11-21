# AbyssGuard 26.3 — Staff GUI Alpha (source)

This is an **experimental source build**, not a verified production release. It extends the prior GitHub Actions-green Alpha. Run GitHub Actions to confirm Minecraft 26.3 API compatibility. **Do not deploy on a production server until testing.**

## Features included in source

- `/rg menu` selects the highest-priority region containing the player.
- `/rg menu <id>` selects a region by name from anywhere.
- 54-slot chest GUI with flag toggles and page navigation; click green/red pane to change `allow`/`deny`, autosaves region JSON. Text flags remain command-only.
- `config/abyssguard/message.txt` auto-generated with HEX gradient prefix and editable messages. `/rg reload` reloads text config.
- `config/abyssguard/protectii.txt` auto-generated with defaults applied on **new** `/rg define` regions. Changes to defaults do not overwrite existing region flags.
- LuckPerms node checks, fail-closed if LuckPerms missing, for commands and block breaking / interacting.

## LuckPerms nodes (assign manually)

- `abyssguard.region.info` — `/rg info`, `/rg list`
- `abyssguard.region.create` — `/rg pos1`, `/rg pos2`, `/rg define`
- `abyssguard.region.delete` — `/rg delete`
- `abyssguard.region.edit` — `/rg flag`, `/rg priority`, owners, members, GUI toggles
- `abyssguard.menu` — `/rg menu` (also needs `abyssguard.region.edit` to change flags)
- `abyssguard.build` — bypass block breaking / placing (note: placement protection callback is NOT yet implemented)
- `abyssguard.interact` — bypass interaction protection
- `abyssguard.admin` — full administrative access and bypass

Example: `/lp group admin permission set abyssguard.menu true` and `/lp group admin permission set abyssguard.region.edit true`.

## Important limitations

**The GUI and new Minecraft 26.3 API usage are NOT yet compilation-tested.** The previous version built, but this is new source. This build is not full WorldGuard parity. In particular, the event hooks for block placement, mob spawning, PvP, TNT, fire, liquids, and many other flags **are not implemented**. Configured flags for those events are stored, but do not enforce the rule yet. Only block-break and use-block have callbacks from the prior Alpha. The GUI visualizes flags but does not prove that those flags are enforced.

The GUI uses server-side vanilla chest menus; no client mod should be required. The current design has no special protection for inventory drag interactions beyond blocked clicks and quick move; test carefully.

If the workflow fails, provide the **first compiler errors** so the Minecraft 26.3 menu API can be adjusted.
