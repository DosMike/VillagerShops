package de.dosmike.sponge.vshop.systems;

import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.shops.ShopEntity;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class LedgerManager {

    private static final Map<UUID, Set<Transaction>> spamless = new HashMap<>(); //mapping shopID to transactions that happened for that shop

    public static void backstuffChat(Transaction transaction) {
        synchronized (spamless) {
            spamless.computeIfAbsent(transaction.vendor, k->new HashSet<>()).add(transaction);
        }
    }

    static class DataCollector implements Comparable<DataCollector> {
        int amount;
        Double money;
        final Currency currency;

        public DataCollector(Transaction first) {
            currency = first.currency;
            amount = first.amount;
            money = first.payed;
        }

        void collect(Transaction data) {
            amount += data.amount;
            money += data.payed;
        }

        Text toText(ItemType type, Locale locale) {
            boolean gain = money >= 0;
            return Text.of(
                    (gain ? TextColors.GREEN : TextColors.RED),
                    (gain ? "+" : ""), Utilities.nf(money, locale), currency.getSymbol(),
                    TextColors.RESET, gain ? " " : " -", amount, " ",
                    type.getTranslation().get(locale)
            );
        }

        @Override
        public int compareTo(DataCollector o) {
            return Integer.compare(amount, o.amount);
        }
    }

    public static void dumpChat() {
        synchronized (spamless) {
            for (Entry<UUID, Set<Transaction>> entry : spamless.entrySet()) {
                Optional<ShopEntity> shopEntity = VillagerShops.getShopFromShopId(entry.getKey());
                if (!shopEntity.isPresent()) continue; //deleted vendor
                if (!shopEntity.get().getShopOwner().isPresent()) continue; //not playershop
                Player online = Sponge.getServer().getPlayer(shopEntity.get().getShopOwner().get()).orElse(null);

                Map<Currency, Double> income = new HashMap<>(); //currency -> money
                Map<ItemType, DataCollector> data = new HashMap<>();
                for (Transaction t : entry.getValue()) {
                    if (income.containsKey(t.currency)) {
                        income.put(t.currency, income.get(t.currency) + t.payed);
                    } else {
                        income.put(t.currency, t.payed);
                    }

                    ItemType type = Sponge.getRegistry().getType(ItemType.class, t.itemID).orElse(ItemTypes.NONE);
                    if (data.containsKey(type))
                        data.get(type).collect(t);
                    else
                        data.put(type, new DataCollector(t));
                }
                List<Text> items = data.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                        .filter(idl -> idl.getValue().amount != 0 && idl.getValue().money != 0) //filter transactions, that cancel each other out
                        .map((Entry<ItemType, DataCollector> idl) -> idl.getValue().toText(idl.getKey(), Utilities.playerLocale(online)))
                        .collect(Collectors.toList());
                if (items.isEmpty()) continue; //all transactions cancelled each other out, so basically nothing changed
                List<Text> icl = income.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                        .map((Entry<Currency, Double> ice) -> Text.of((ice.getValue() < 0 ? TextColors.RED : TextColors.GREEN), (ice.getValue() < 0 ? ice.getValue().toString() : "+" + ice.getValue()), ice.getKey().getSymbol(), TextColors.RESET))
                        .collect(Collectors.toList());

                if (online != null)
                    online.sendMessage(VillagerShops.getTranslator().localText("shop.chat.transaction.base")
                        .replace("%shop%", Transaction.shopText(entry.getKey()))
                        .replace("%items%", Text.joinWith(Text.of(", "), items))
                        .replace("%money%", Text.joinWith(Text.of(", "), icl))
                        .resolve(online).orElse(Text.of("[Chat transaction notification]"))
                );
            }
            spamless.clear();
        }
    }

    public static class Transaction {
        UUID customer, vendor; //customer is player uuid, vendor is shop uuid
        String itemID;
        int amount;
        Double payed;
        Instant timestamp;
        Currency currency;
        private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public Transaction(UUID customer, UUID vendor, Double money, Currency currency, ItemType type, int amount) {
            this.customer = customer;
            this.vendor = vendor;
            this.itemID = type.getId();
            this.amount = amount;
            this.payed = money;
            this.currency = currency;
            timestamp = Instant.now();
        }

        private Transaction() {
        }

        public String toString() {
            return toText(Sponge.getServer().getConsole()).toPlain();
        }

        public Text toText(CommandSource viewer) {
            Optional<ItemType> type = Sponge.getRegistry().getType(ItemType.class, itemID);
            ItemStack display;
            String displayName = "???";
            if (type.isPresent()) {
                display = ItemStack.of(type.get(), amount);
                displayName = display.getType().getTranslation().get(Utilities.playerLocale(viewer));
            } else {
                display = ItemStack.builder()
                        .itemType(ItemTypes.GLASS)
                        .add(Keys.DISPLAY_NAME, Text.of("???"))
                        .add(Keys.ITEM_LORE, Collections.singletonList(Text.of(TextColors.WHITE, "ID: ", itemID)))
                        .build();
            }
//			Currency c = VillagerShops.getInstance().CurrencyByName(currency);
            boolean gain = payed >= 0;
            Calendar formatCalendar = new GregorianCalendar();
            formatCalendar.setTimeInMillis(timestamp.toEpochMilli());

            return VillagerShops.getTranslator().localText("shop.ledger.entry." + (gain ? "gain" : "loss"))
                    .replace("%customer%", userText(customer))
                    .replace("%vendor%", shopText(vendor))
                    .replace("%amount%", amount)
                    .replace("%item%", Text.builder(displayName)
                            .onHover(TextActions.showItem(display.createSnapshot()))
                            .style(TextStyles.ITALIC)
                            .build())
                    .replace("%price%", gain ? payed : -payed)
                    .replace("%currency%", currency.getSymbol())
                    .replace("%timestamp%", format.format(formatCalendar.getTime()))
                    .replace("\\n", Text.NEW_LINE)
                    .resolve(viewer).orElse(Text.of("[Transaction]"));
        }

        static Text shopText(UUID shopId) {
            Optional<ShopEntity> shopEntity = VillagerShops.getShopFromShopId(shopId);
            if (!shopEntity.isPresent()) return Text.of(TextColors.GRAY, "???", TextColors.RESET);
            Text.Builder builder = Text.builder().append(shopEntity.get().getDisplayName());
            if (!shopEntity.get().getShopOwner().isPresent())
                builder.onHover(TextActions.showText(Text.of(
                        TextColors.RED, "admin", Text.NEW_LINE,
                        TextColors.WHITE, "UUID: ", TextColors.GRAY, shopEntity.get().getIdentifier().toString()
                )));
            else {
                UUID ownerId = shopEntity.get().getShopOwner().get();
                Optional<User> user = VillagerShops.getUserStorage().get(ownerId);
                if (!user.isPresent())
                    builder.onHover(TextActions.showText(Text.of(
                            TextColors.WHITE, "Owner: ", TextColors.GRAY, "Unknown", Text.NEW_LINE,
                            TextColors.WHITE, "Owner UUID: ", TextColors.GRAY, ownerId.toString(), Text.NEW_LINE,
                            TextColors.WHITE, "UUID: ", TextColors.GRAY, shopEntity.get().getIdentifier().toString()
                    )));
                else builder.onHover(TextActions.showText(Text.of(
                        TextColors.WHITE, "Owner: ", TextColors.GRAY, user.get().getName(), Text.NEW_LINE,
                        TextColors.WHITE, "Owner UUID: ", TextColors.GRAY, ownerId.toString(), Text.NEW_LINE,
                        TextColors.WHITE, "Last Seen: ",
                        (user.get().isOnline()
                                ? Text.of(TextColors.GREEN, "Online")
                                : Text.of(TextColors.GRAY, user.get().get(Keys.LAST_DATE_PLAYED).orElse(Instant.now()).toString()))
                        , Text.NEW_LINE,
                        TextColors.WHITE, "UUID: ", TextColors.GRAY, shopEntity.get().getIdentifier().toString()
                )));
            }
            return Text.of(builder.build(), TextColors.RESET);
        }

        static Text userText(UUID playerId) {
            Optional<User> user = VillagerShops.getUserStorage().get(playerId);
            return user.map(value -> Text.of(Text.builder(value.getName()).color(TextColors.BLUE)
                    .onHover(TextActions.showText(Text.of(
                            TextColors.WHITE, "UUID: ", TextColors.GRAY, playerId.toString(),
                            TextColors.WHITE, "Last Seen: ",
                            (value.isOnline()
                                    ? Text.of(TextColors.GREEN, "Online")
                                    : Text.of(TextColors.GRAY, value.get(Keys.LAST_DATE_PLAYED).orElse(Instant.now()).toString()))
                    ))).build(), TextColors.RESET)).orElseGet(() ->
                    Text.of(Text.builder("Unknown").color(TextColors.GRAY)
                            .onHover(TextActions.showText(Text.of(TextColors.WHITE, "UUID: ", TextColors.GRAY, playerId.toString())))
                            .build(), TextColors.RESET));
        }

        public static Transaction fromDatabase(ResultSet args) throws SQLException {
            Transaction result = new Transaction();

            result.customer = UUID.fromString(args.getString("customer"));
            result.vendor = UUID.fromString(args.getString("vendor"));
            result.itemID = args.getString("item");
            result.amount = args.getInt("amount");
            result.payed = args.getDouble("price");
            result.currency = Utilities.CurrencyByName(args.getString("currency"));
            result.timestamp = args.getTimestamp("date").toInstant();

            return result;
        }

        /**
         * returns success
         */
        public void toDatabase() {
            VillagerShops.getAsyncScheduler().execute(() -> {
                try (Connection con = getDataSource().getConnection();
                     PreparedStatement statement = con.prepareStatement("INSERT INTO `vshopledger`(`customer`, `vendor`, `item`, `amount`, `price`, `currency`) VALUES (?,?,?,?,?,?);")) {
                    con.setAutoCommit(true);
                    statement.setString(1, customer.toString());
                    statement.setString(2, vendor.toString());
                    statement.setString(3, itemID);
                    statement.setInt(4, amount);
                    statement.setDouble(5, payed);
                    statement.setString(6, currency.getId());
                    statement.execute();
                } catch (SQLException e) {
                    VillagerShops.w("Saving a player shop transaction to the ledger database failed: (%d) %s", e.getErrorCode(), e.getMessage());
                    e.printStackTrace();
                }
            });

        }
    }

    private static SqlService sql = null;
    private static final String DB_URL = "jdbc:h2:./config/vshop/ledger.db";

    public static DataSource getDataSource() throws SQLException {
        if (sql == null) {
            sql = Sponge.getServiceManager().provide(SqlService.class).get();
            try (Connection con = sql.getDataSource(DB_URL).getConnection()) {
                con.setAutoCommit(true);
                con.prepareStatement("CREATE TABLE IF NOT EXISTS `vshopledger` (\n" +
                        "`ID` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,\n" +
                        "`customer` varchar(40) NOT NULL,\n" +
                        "`vendor` varchar(40) NOT NULL,\n" +
                        "`item` varchar(255) NOT NULL,\n" +
                        "`amount` int(11) NOT NULL,\n" +
                        "`price` double NOT NULL,\n" +
                        "`currency` varchar(40) NOT NULL,\n" +
                        "`date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP\n" +
                        ");").execute();
            }
        }
        return sql.getDataSource(DB_URL);
    }

    public static Future<List<Transaction>> getLedgerFor(User user) {
        return VillagerShops.getAsyncScheduler().submit(() -> {
            String sql1 = "SELECT `customer`, `vendor`, `item`, `amount`, `price`, `currency`, `date` FROM `vshopledger` WHERE `vendor`=? ORDER BY `ID` DESC LIMIT 250;";
            List<Transaction> transactions = new LinkedList<>();
            for (ShopEntity npc : VillagerShops.getShops())
                if (npc.isShopOwner(user.getUniqueId()))
                    try (Connection conn = getDataSource().getConnection();
                         PreparedStatement stmt = conn.prepareStatement(sql1)) {

                        stmt.setString(1, npc.getIdentifier().toString());
                        ResultSet results = stmt.executeQuery();
                        while (results.next()) {
                            transactions.add(Transaction.fromDatabase(results));
                        }
                        results.close();
                    } catch (SQLException e) {
                        VillagerShops.getSyncScheduler().execute(e::printStackTrace);
                    }
            return transactions;
        });
    }

    /**
     * return success
     */
    public static void openLedgerFor(CommandSource viewer, User target) {
        VillagerShops.getAsyncScheduler().execute(() -> {
            List<Transaction> raw;
            try {
                raw = getLedgerFor(target).get();
                List<Text> pages = new LinkedList<>();
                int entriesPerPage = (viewer instanceof Player) ? 3 : 10;
                int i = 0;
                Text.Builder page = Text.builder();
                for (Transaction t : raw) {
                    if (i >= entriesPerPage) {
                        i = 0;
                        pages.add(page.build());
                        page = Text.builder();
                    }
                    if (i > 0) page.append(Text.NEW_LINE);
                    page.append(t.toText(viewer));
                    i++;
                }
                if (i > 0) pages.add(page.build());

                VillagerShops.getSyncScheduler().execute(() -> { //post back to main thread
                    if (viewer instanceof Player) {
                        ((Player) viewer).sendBookView(BookView.builder()
                                .title(Text.of("Business Ledger for ", target.getName()))
                                .addPages(pages)
                                .build()
                        );
                    } else {
                        PaginationList.builder()
                                .title(Text.of("Business Ledger for ", target.getName()))
                                .linesPerPage(20)
                                .contents(pages)
                                .build()
                                .sendTo(viewer);
                    }
                });
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }
}
