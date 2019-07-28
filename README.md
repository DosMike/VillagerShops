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

Latest Version: 2.1 (2019-07-28)<br>
(Written, compiled and tested for spongevanilla-1.12.2-7.1.6)

Commands
-----

For every edit command but create, please look at the shop  
(The commands are written as Usage Messages, you can google docopt for more information)

- `/vshop create [--at <World/X/Y/Z/Rotation>] <EntityType> [--skin <Skin>] [DisplayName]`
- `/vshop delete` (deletes shop)
- `/vshop add [-l <Limit>] <~|BuyPrice> <~|SellPrice> [Currency] [-o <Index>] [--nbt <ignore_nbt|ignore_damage|normal>]`
- `/vshop remove <Index>` (removes item from shop)
- `/vshop link` (connect a playershop to a different chest)
- `/vshop identify` or `/vshop id`
- `/vshop save`
- `/vshop reload` (reapply skins on sponge:human entities)
- `/vshop ledger [Player]` or `/vshop log [Player]` (display last 100 transactions the players shops made)
- `/vshop list [Player]` (list all shops, offer invsee for player shops and tp)
- `/vshop tphere <Shop ID>` moves a shop by id (`/vshop identify`) to your current location

Permissions
-----
- `vshop.edit.admin` edit any shop, create admin shops, `/vshop save`, `/vshop reload`, `/vshop list`, `/vshop tphere`
- `vshop.edit.player` is required to create your player shop
- the add, remove and delete command are always available for shops you own
- `vshop.create.<ENTITYTYPE>` is required for each entity type a player should be allowed to create, where colons are replaced with dots and dashed are taken out. so for example `minecraft:villager`s would require `vshop.create.minecraft.villager`
- `vshop.edit.identify` for `/vshop identify`
- `vshop.edit.linkchest` for `/vshop link`
- `vshop.edit.move` for `/vshop tphere`
- `vshop.ledger.base` for `/vshop ledger`
- `vshop.ledger.others` for `/vshop ledger <Player>`

Options
-----
With LuckPerms you can set options like `/lp user DosMike meta set KEY VALUE`

- `vshop.option.playershop.limit` Number of player-shops one is allowed to have
- `vshop.option.dailyincome.limit` Maximum money a player may earn through admin shops per irl day, reset at 24:00 server time
- `vshop.option.chestlink.distance` Maximum distance a player-shop may have to it's stock-container. This affects `/vshop link` as well as `/vshop tphere`

Player Shops
-----
In order to create a player shop you'll have to first place a container below the block you want the shop to be. This can either be directly below or with a one block spacing.

The chest should be protected from punching, explosions and is only accessible for the owner. **Double chest wont work - The plugin does not care for the other half, neither stock nor protection wise.**

On forge this means that a chest has to have at least 27 slots, since I do not want shops to be able to sell / buy into machines.

More Inforamtion
-----
The plugin will create two configs and a h2 database.   
**None of these files are meant to be edited by you**

The `vshop.conf` will store all shops including their listings. The income per player for the day are stored in `incomeLimits.conf` to persist over server restarts. All shop transactions are saved in the `ledger.db.mv.db` - you can inspect them with a h2 database editor. The database is never cleared at the moment so you may want to delete or trim it every now and then.

Chat messages will notify playershop owners about purchases in all their shops every 15 seconds to prevent mass spam.

Shops save with the world now, this means that editing shops via config got extremely unreliable and made the old behaviour of `/vshop reload` useless. Instead this command will now try to reload skins for `sponge:human` shops.   
Even though I set the skin correctly they sometimes seem to not load/be sent to the player, so `/vshop reload` will reapply the skin.