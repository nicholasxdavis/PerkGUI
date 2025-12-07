<td style="text-align: center;">

<img src="https://nicholasxdavis.github.io/BN-db1/img/perks.png" width="600" height="300" alt="PerkShop Banner" />

PerkShop 1.0.0

PandoraPlugins by Blacnova

<br />

<a href="#"> <img src="https://img.shields.io/badge/Version-1.0.0-skyblue?style=for-the-badge" alt="Current Version"> </a>

<br /><hr />

About PerkShop

PerkShop is a comprehensive shop system for Spigot/Paper 1.21 servers that allows players to purchase permanent abilities, command access, and stat boosts. It features a GUI-based shop, an integrated quest system, and rewards for gameplay activities. The plugin integrates seamlessly with Vault for economy, LuckPerms for permissions, and supports LevelPlugin for progression requirements.

Key Features

GUI Perk Shop: A clean, navigable interface (/shop) to browse and purchase perks.

Command Perks: Unlock access to powerful commands like Heal, Feed, Repair, and Night Vision.

Passive Abilities: Purchase permanent effects like Speed, Jump Boost, Water Breathing, Mining Speed, and Extra Hearts (+6 HP).

Quest System: Built-in quests (Kill Mobs, Mine Blocks) that reward players with currency upon completion.

Reward System: Earn money automatically by performing actions like mining ores, killing mobs, fishing, and placing specific blocks.

Progression: Lock powerful perks behind player levels (requires LevelPlugin) and other perk dependencies.

</td>

<br /><hr />

Commands

/shop, /perkshop | Opens the Perk Shop GUI to buy new perks.

/perks | Shows your list of purchased perks.

/heal | Restores health and hunger (Requires 'Heal' perk).

/feed | Restores hunger and saturation (Requires 'Feed' perk).

/nv, /nightvision | Sets your personal time to night (Requires 'Night' perk).

/repair | Repairs the item in your main hand (Requires 'Repair' perk).

Dependencies

To run PerkShop, your server requires the following:

Java 21 or higher.

Spigot/Paper 1.21 or compatible fork.

Vault (Required for economy transactions).

LevelPlugin (Optional, for level-based purchase requirements).

LuckPerms (Recommended for managing permissions).

Essentials (Optional, for improved respawn handling).

Configuration

The plugin is highly configurable via config.yml. You can adjust perk costs, cooldowns, materials, and rewards.

Example Perk Configuration

perks:
heal:
name: "&6Heal"
material: "GOLDEN_APPLE"
description:
- "&7Heal yourself with /heal"
- "&7Cooldown: &6{cooldown} &7seconds"
cost: 75000.0
required-level: 30
permission: "perkshop.heal"
cooldown: 300
enabled: true

repair:
name: "&6Repair"
material: "ANVIL"
description:
- "&7Repair items in hand with /repair"
- "&7Cooldown: &6{cooldown} &7seconds"
cost: 100000.0
required-level: 40
permission: "perkshop.repair"
cooldown: 300
enabled: true

License

Copyright © 2025 Blacnova Development

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see https://www.gnu.org/licenses/.

<br /><hr />

<td style="text-align: center;">

Support & Links

Report Bugs • View Source

</td>
