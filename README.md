# AmplProtections

Advanced region protection system for Minecraft Paper 1.21 servers.

## What is AmplProtections?

AmplProtections lets players claim and protect areas in the world using placeable protection blocks. Each block creates a protected region with configurable radius, granular flag permissions, and a full GUI management system.

## Features

### Protection System
- **4 protection tiers** — Carbon, Iron, Diamond, Emerald (configurable via `blocks.yml`)
- **Per-world limits** and **per-type limits** with rank-based permissions
- **Collision detection** prevents overlapping regions
- **Hidden blocks** option — protection block becomes invisible after placement

### Flag Permissions
- **5-level permission system** — Nobody, Owner, Members, Admins, Everyone
- **34+ configurable flags** — PvP, mob damage, block break/place, doors, chests, vehicles, and more
- **Environmental flags** — fire spread, lava flow, ice melt, crop trample, etc.
- **Silent mode** — suppress access-denied messages per flag

### Members & Ranks
- **4-tier rank system** — Owner, Secondary Owner, Admin, Member
- Promote/demote members with cascading permissions
- Secondary Owners can manage members independently

### GUI Menus
- **12 fully configurable menus** — main, members, flags, buy, merge, presets, rentals, rollback, search, and more
- **Anvil search** and **chat search** for finding players
- Admin panel with server-wide protection overview

### Merge System
- Merge adjacent same-type protections into larger regions
- Configurable max radius and merge cost
- Full unmerge on deletion — original protections are restored

### Economy
- Vault integration for protection purchases
- Per-type pricing, tax system for rentals
- Bypass permission for free purchases

### Rentals
- List protections for rent with configurable price and duration
- Auto-renew with tax deduction
- Owner receives payment minus tax

### Rollback
- **Admin-only** block change logging per protection
- Rollback by player or entire region
- Preview changes before reverting
- Configurable log retention (max age days)

### Holograms & Glow
- Floating text displays above protections with live data
- Configurable content template with placeholders
- Glow effect on protection preview with color options

### Presets
- Built-in presets — Public, Private, Friends
- Per-player custom presets saved to database
- One-click flag configuration

### Multilingual
- Full English and Spanish support
- Configurable language via `config.yml`
- All messages use MiniMessage format

## Requirements

- Paper 1.21+
- MySQL database
- Java 21

## Optional Dependencies

- **PlaceholderAPI** — external placeholder support
- **Vault** — economy integration

## Commands

| Command | Description | Permission |
|---|---|---|
| `/p menu` | Open protection GUI | `amplprotections.use` |
| `/p info` | View current protection info | `amplprotections.use` |
| `/p add <player>` | Add member | Owner/Admin |
| `/p remove <player>` | Remove member | Owner/Admin |
| `/p promote <player>` | Promote member | Owner/Secondary |
| `/p demote <player>` | Demote member | Owner/Secondary |
| `/p flag <flag> <level>` | Change flag permission | Owner |
| `/p view` | Show protection boundaries | `amplprotections.use` |
| `/p list` | List your protections | `amplprotections.use` |
| `/p buy` | Open protection shop | `amplprotections.use` |
| `/p merge` | Merge adjacent protections | `amplprotections.merge.use` |
| `/p preset` | Manage flag presets | `amplprotections.preset.use` |
| `/p hologram` | Toggle hologram | `amplprotections.hologram.toggle` |
| `/p rent` | Manage rentals | `amplprotections.rent.use` |
| `/p rollback` | Revert changes | `amplprotections.rollback.admin` |
| `/p lore <text>` | Set custom description | Owner |
| `/aprot give <player> <type>` | Give protection item | `amplprotections.admin.use` |
| `/aprot list` | Admin protection browser | `amplprotections.admin.use` |
| `/aprot reload` | Reload configuration | `amplprotections.admin.use` |
