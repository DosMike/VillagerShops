VillagerShops
=====

### [This plugin requires LangSwitch](https://github.com/DosMike/LangSwitch)  
Feel free to open an issue on the git to share your translations


**If you're updating from 1.x to 2.x you'll have to delete ledger db in config/vshop.**
### [Version 2.0+ requires MegaMenus](https://ore.spongepowered.org/DosMike/Mega-Menus)

Description:
-----

This plugin allows you to set up villagers or other mobs as shops.  
They will sell items as **admin shop** (this means infinite stock), or as **player shops** if placed above a chest.  
The shop will be displayed as inventory over multiple pages you can modify with chat commands as admin or if you own the shop.  
Buy items with a primary click and sell them with a secondary click (usually primary is the left mouse button).  
You can also name the shop mobs and set the variant (Villager profession).  
Additionally you can set items to ignore damage, nbt or use oredict entries.  
*Please note that your server needs any economy Plugin for this to work*  

Latest Version: 2.2.1 (2019-08-14)<br>
(Written, compiled and tested for spongevanilla-1.12.2-7.2.0)

Commands
-----

For every edit command but create, please look at the shop  
(The commands are written as Usage Messages, you can google docopt for more information)

- `/vshop create [--at <World/X/Y/Z/Rotation>] <EntityType> [--skin <Skin>] [DisplayName]`
- `/vshop import` (use an existing entity as shop)
- `/vshop delete` (deletes shop)
- `/vshop add [-l <Limit>] <~|BuyPrice> <~|SellPrice> [Currency] [-o <Index>] [--nbt <ignore_nbt|ignore_damage|oredict|normal>]`
- `/vshop remove <Index>` (removes item from shop)
- `/vshop link` (connect a playershop to a different chest)
- `/vshop identify` or `/vshop id`
- `/vshop save`
- `/vshop reload [--translations]` (reload settings.conf, --translations forces an update to translation files)
- `/vshop ledger [Player]` or `/vshop log [Player]` (display last 100 transactions the players shops made)
- `/vshop list [Player]` (list all shops, offer invsee for player shops and tp)
- `/vshop tphere <Shop ID>` moves a shop by id (`/vshop identify`) to your current location

Permissions
-----
- `vshop.edit.admin` edit any shop, create admin shops, `/vshop save`, `/vshop reload`, `/vshop list`, `/vshop tphere`
- `vshop.edit.player` is required to create your player shop
- `vshop.edit.import` is required to turn existing entities into shops with `/vshop import`
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

(In-)Compatibility
-----

* **Pixelmon** will replace various vanilla mobs with Pixelmon versions resulting in VillagerShops spamming mobs
* **InventoryTweaks** has to be installed client side only - otherwise item dupes may happen

External Connections
-----

**[Version Checker](https://github.com/DosMike/SpongePluginVersionChecker)**  
This plugin uses a version checker to notify you about available updates.  
This updater is **disabled by default** and can be enabled in `config/vshop/settings.conf`
by setting the value `VersionChecker` to `true`.    
If enabled it will asynchronously check (once per server start) if the Ore repository has any updates.  
This will *only print update notes into the server log*, no files are being downlaoded!

**Translation Downloads**  
If you don't want to download translations from GtiHub or can't find them you can turn on
AutoDownloading in the `config/vshop/settings.conf`. If you start the server or `/vshop reload`
with this setting enabled and no tranlations can be found on your server it will automatically 
pull the latest translation files from GitHub and place them into `config/vshop/Lang` for you. 
*Once the downloads finish you'll have to restart the server.*