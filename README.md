VillagerShops
=====

### [This plugin requires LangSwitch](https://github.com/DosMike/LangSwitch)

Feel free to open an issue on the git to share your translations

Description:
-----

This plugin allows you to set up villagers or other mobs as shops.<br>
They will sell items as **admin shop** (this means infinite stock), or as **player shops** if placed above a chest.<br>
The shop will be displayed as two row inventory you can modify with chat commands as admin or if you own the shop.<br>
Buy items from the top row and sell them at the bottom row.<br>
You can also name the shop mobs and set the variant (Villager profession).<br>
*Please note that you require any economy Plugin for this to work*<br>
<sub>Let's just never mention the Bukkit-version...</sub>

Latest Version: 1.1 (2017-08-14)<br>
(Written, compiled and tested for spongevanilla-1.12-7.0.0-BETA-306)

Commands
-----

For every command but create, please look at the shop

- `/vshop create [--at <World/X/Y/Z/Rotation>] <EntityType> [--skin <Skin>] [DisplayName]`
- `/vshop delete` (deletes shop)
- `/vshop add [-l <Limit>] <-|BuyPrice> <-|SellPrice> [Currency]`
- `/vshop remove <Index>` (removes item from shop)
- `/vshop link`
- `/vshop identify` or `/vshop id`
- `/vshop save`
- `/vshop reload`

Permissions
-----
- `vshop.edit.admin` use any of the above commands to create admin and player shops
- `vshop.edit.player` is required to create your player shop
- the add, remove and delete command are always available for shops you own
- `vshop.create.<ENTITYTYPE>` is required for each entity type a player should be allowed to create, where colons are replaced with dots and dashed are taken out. so for example `minecraft:villager`s would require `vshop.create.minecraft.villager`
- `vshop.edit.identify` for `/vshop identify`
- `vshop.edit.linkchest` for `/vshop link`

Options
-----
With LuckPerms you can set options like `/lp user DosMike meta set KEY VALUE`

- `vshop.option.playershop.limit` Number of player-shops one is allowed to have

Player Shops
-----
In order to create a player shop you'll have to first place a chest (regular) below the block you want the shop to be. This can either be directly below or with a one block spacing.

The chest should be protected from punching, explosions and is only accessible for the owner. Double chest wont even care for the other half, neither stock nor protection wise.