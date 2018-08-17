package de.dosmike.sponge.vshop;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.sql.DataSource;

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

public class LedgerManager {
	
	private static Map<UUID, Set<Transaction>> spamless = new HashMap<>(); //mapping shopID to transactions that happened for that shop
	public static void backstuffChat(Transaction transaction) {
		synchronized(spamless) {
			Set<Transaction> v;
			if (spamless.containsKey(transaction.vendor)) {
				v = spamless.get(transaction.vendor);
			} else {
				v = new HashSet<>();
			}
			v.add(transaction);
			spamless.put(transaction.vendor, v);
		}
	}
	static class DataCollector implements Comparable<DataCollector>{
		int amount;
		Double money;
		Currency currency;
		public DataCollector(Transaction first) {
			currency = first.currency;
			amount = first.amount;
			money = first.payed;
		}
		void collect(Transaction data) {
			amount += data.amount;
			money += data.payed;
		}
		Text toText(ItemType type) {
			boolean gain = money>=0;
			return Text.of(
					(gain?TextColors.GREEN:TextColors.RED), 
					(gain?"+"+money:money.toString()), currency.getSymbol(), 
					TextColors.RESET, gain?" ":" -", amount, " ", 
					type.getName()
					);
		}
		@Override
		public int compareTo(DataCollector o) {
			return Integer.compare(amount, o.amount);
		}
	}
	public static void dumpChat() {
		synchronized(spamless) {
			for (Entry<UUID, Set<Transaction>> e : spamless.entrySet()) {
				Optional<NPCguard> vendor = VillagerShops.getNPCfromShopUUID(e.getKey());
				if (!vendor.isPresent()) continue; //deleted vendor
				if (!vendor.get().getShopOwner().isPresent()) continue; //not playershop
				Optional<User> owner = VillagerShops.getUserStorage().get(vendor.get().getShopOwner().get());
				if (!owner.isPresent() || !owner.get().isOnline()) continue; //player no more present or disconnected
				Player online = owner.get().getPlayer().get();
				
				Map<Currency, Double> income = new HashMap<>(); //currency -> money
				Map<ItemType, DataCollector> data = new HashMap<>();
				for (Transaction t : e.getValue()) {
					if (income.containsKey(t.currency)) {
						income.put(t.currency, income.get(t.currency)+t.payed);
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
					.filter(idl->idl.getValue().amount!=0 && idl.getValue().money!=0) //filter transactions, that cancel each other out
					.map((Entry<ItemType, DataCollector> idl)->idl.getValue().toText(idl.getKey()))
					.collect(Collectors.toList());
				if (items.isEmpty()) continue; //all transactions cancelled each other out, so basically nothing changed
				List<Text> icl = income.entrySet().stream()
					.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
					.map((Entry<Currency, Double> ice)->Text.of((ice.getValue()<0?TextColors.RED:TextColors.GREEN),(ice.getValue()<0?ice.getValue().toString():"+"+ice.getValue()),ice.getKey().getSymbol(),TextColors.RESET ))
					.collect(Collectors.toList());
				
				online.sendMessage(VillagerShops.getTranslator().localText("shop.chat.transaction.base")
						.replace("%shop%", Transaction.shopText(e.getKey()))
						.replace("%items%", Text.joinWith(Text.of(", "), items))
						.replace("%money%", Text.joinWith(Text.of(", "), icl))
						.resolve(online).orElse(Text.of("[Chat transaction notification]"))
						);
			}
			spamless.clear();
		}
	}
	
	static class Transaction {
		UUID customer, vendor; //customer is player uuid, vendor is shop uuid
		String itemID;
		int amount;
		Double payed;
		int slot;
		Instant timestamp;
		Currency currency;
		private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		public Transaction(UUID customer, UUID vendor, Double money, Currency currency, int slot, ItemType type, int amount) {
			this.customer = customer;
			this.vendor = vendor;
			this.itemID = type.getId();
			this.amount = amount;
			this.slot = slot;
			this.payed = money;
			this.currency = currency;
			timestamp = Instant.now();
		}
		private Transaction() {}
		
		public String toString() {
			return toText(Sponge.getServer().getConsole()).toPlain();
		}
		
		public Text toText(CommandSource viewer) {
			Optional<ItemType> type = Sponge.getRegistry().getType(ItemType.class, itemID);
			ItemStack display=null;
			String displayName = "???";
			if (type.isPresent()) {
				display = ItemStack.of(type.get(), amount);
				displayName = display.getType().getName();
			} else {
				display = ItemStack.builder()
					.itemType(ItemTypes.GLASS)
					.add(Keys.DISPLAY_NAME, Text.of("???"))
					.add(Keys.ITEM_LORE, Arrays.asList(new Text[]{ Text.of(TextColors.WHITE, "ID: ", itemID) }))
					.build();
			}
//			Currency c = VillagerShops.getInstance().CurrencyByName(currency);
			boolean gain = payed>=0;
			Calendar formatCalendar = new GregorianCalendar();
			formatCalendar.setTimeInMillis(timestamp.toEpochMilli());
			
			return VillagerShops.getTranslator().localText("shop.ledger.entry."+(gain?"gain":"loss"))
					.replace("%customer%", userText(customer))
					.replace("%vendor%", shopText(vendor))
					.replace("%amount%", amount)
					.replace("%item%", Text.of(TextStyles.ITALIC, Text.builder(displayName)
							.onHover(TextActions.showItem(display.createSnapshot()))
							.build(), "§r")) //reset won't properly work for me for some reason
					.replace("%index%", slot)
					.replace("%price%", gain?payed:-payed)
					.replace("%currency%", currency.getSymbol())
					.replace("%timestamp%", format.format(formatCalendar.getTime()))
					.replace("\\n", "\n")
					.resolve(viewer).orElse(Text.of("[Transaction]"));
		}
		static Text shopText(UUID shop) {
			Optional<NPCguard> vendor = VillagerShops.getNPCfromShopUUID(shop);
			if (!vendor.isPresent()) return Text.of(TextColors.GRAY, "???", TextColors.RESET);
			Text.Builder builder = Text.builder().append(vendor.get().getDisplayName());
			if (!vendor.get().getShopOwner().isPresent())
				builder.onHover(TextActions.showText(Text.of(
						TextColors.RED, "admin", Text.NEW_LINE,
						TextColors.WHITE, "UUID: ", TextColors.GRAY, vendor.get().getIdentifier().toString()
						)));
			else {
				UUID oid = vendor.get().getShopOwner().get();
				Optional<User> user = VillagerShops.getUserStorage().get(oid);
				if (!user.isPresent()) 
						builder.onHover(TextActions.showText(Text.of(
								TextColors.WHITE, "Owner: ", TextColors.GRAY, "Unknown", Text.NEW_LINE,
								TextColors.WHITE, "Owner UUID: ", TextColors.GRAY, oid.toString(), Text.NEW_LINE,
								TextColors.WHITE, "UUID: ", TextColors.GRAY, vendor.get().getIdentifier().toString()
								)));
				else builder.onHover(TextActions.showText(Text.of(
								TextColors.WHITE, "Owner: ", TextColors.GRAY, user.get().getName(), Text.NEW_LINE,
								TextColors.WHITE, "Owner UUID: ", TextColors.GRAY, oid.toString(), Text.NEW_LINE,
								TextColors.WHITE, "Last Seen: ", 
									(user.get().isOnline() 
											? Text.of(TextColors.GREEN, "Online")
											: Text.of(TextColors.GRAY, user.get().get(Keys.LAST_DATE_PLAYED).orElse(Instant.now()).toString()))
									, Text.NEW_LINE,
								TextColors.WHITE, "UUID: ", TextColors.GRAY, vendor.get().getIdentifier().toString() 
						)));
			}
			return Text.of(builder.build(), TextColors.RESET);
		}
		static Text userText(UUID player) {
			Optional<User> user = VillagerShops.getUserStorage().get(player);
			if (!user.isPresent()) return Text.of(Text.builder("Unknown").color(TextColors.GRAY)
					.onHover(TextActions.showText(Text.of(TextColors.WHITE, "UUID: ", TextColors.GRAY, player.toString())))
					.build(), TextColors.RESET);
			else return Text.of(Text.builder(user.get().getName()).color(TextColors.BLUE)
					.onHover(TextActions.showText(Text.of(
							TextColors.WHITE, "UUID: ", TextColors.GRAY, player.toString(),
							TextColors.WHITE, "Last Seen: ", 
								(user.get().isOnline() 
										? Text.of(TextColors.GREEN, "Online")
										: Text.of(TextColors.GRAY, user.get().get(Keys.LAST_DATE_PLAYED).orElse(Instant.now()).toString())) 
					))).build(), TextColors.RESET);
		}
		
		public static Transaction fromDatabase(ResultSet args) throws SQLException {
			Transaction result = new Transaction();
			
			result.customer = UUID.fromString(args.getString("customer"));
			result.vendor = UUID.fromString(args.getString("vendor"));
			result.itemID = args.getString("item");
			result.slot = args.getInt("slot");
			result.amount = args.getInt("amount");
			result.payed = args.getDouble("price");
			result.currency = VillagerShops.getInstance().CurrencyByName(args.getString("currency"));
			result.timestamp = args.getTimestamp("date").toInstant();
			
			return result;
		}
		/** returns success */
		public void toDatabase() {
			VillagerShops.getAsyncScheduler().execute(()->{
				try (Connection con = getDataSource().getConnection();
					PreparedStatement statement = con.prepareStatement("INSERT INTO `vshopledger`(`customer`, `vendor`, `item`, `slot`, `amount`, `price`, `currency`) VALUES (?,?,?,?,?,?,?);")) {
					con.setAutoCommit(true);
					statement.setString(1, customer.toString());
					statement.setString(2, vendor.toString());
					statement.setString(3, itemID);
					statement.setInt(4, slot);
					statement.setInt(5, amount);
					statement.setDouble(6, payed);
					statement.setString(7, currency.getId());
					statement.execute();
				} catch (SQLException e) {
					VillagerShops.w("Saving a player shop transaction to the ledger database failed: (%d) %s", e.getErrorCode(), e.getMessage());
					e.printStackTrace();
				}
			});
			
		}
	}
	
	private static SqlService sql=null;
	private static final String DB_URL = "jdbc:h2:./config/vshop/ledger.db";
	public static DataSource getDataSource() throws SQLException {
	    if (sql == null) {
	        sql = Sponge.getServiceManager().provide(SqlService.class).get();
	        try (Connection con = sql.getDataSource(DB_URL).getConnection();) {
	        	con.setAutoCommit(true);
	        	con.prepareStatement("CREATE TABLE IF NOT EXISTS `vshopledger` (\n"+
	        		"`ID` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,\n"+
	        		"`customer` varchar(40) NOT NULL,\n"+
	        		"`vendor` varchar(40) NOT NULL,\n"+
	        		"`item` varchar(255) NOT NULL,\n"+
	        		"`slot` int(11) NOT NULL,\n"+
	        		"`amount` int(11) NOT NULL,\n"+
	        		"`price` double NOT NULL,\n"+
	        		"`currency` varchar(40) NOT NULL,\n"+
	        		"`date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP\n"+
	        	");").execute();
	        }
	    }
	    return sql.getDataSource(DB_URL);
	}
	
	public static Future<List<Transaction>> getLedgerFor(User user) {
		Future<List<Transaction>> result = //new CompletableFuture<>();
			VillagerShops.getAsyncScheduler().submit(new Callable<List<Transaction>>(){
				@Override
					public List<Transaction> call() throws Exception {
						String sql = "SELECT `customer`, `vendor`, `item`, `slot`, `amount`, `price`, `currency`, `date` FROM `vshopledger` WHERE `vendor`=? ORDER BY `ID` DESC LIMIT 250;";
						List<Transaction> transactions = new LinkedList<>();
						for (NPCguard npc : VillagerShops.getNPCguards()) 
							if (npc.isShopOwner(user.getUniqueId()))
							    try (Connection conn = getDataSource().getConnection();
							         PreparedStatement stmt = conn.prepareStatement(sql)) {
							    	
							    	stmt.setString(1, npc.getIdentifier().toString());
							    	ResultSet results = stmt.executeQuery();
							        while (results.next()) {
							            transactions.add(Transaction.fromDatabase(results));
							        }
							        results.close();
							    } catch (SQLException e) {
							    	VillagerShops.getSyncScheduler().execute(()->e.printStackTrace());
								}
//					    VillagerShops.getSyncScheduler().execute(()->VillagerShops.l("Found %d entries", transactions.size()));
					    return transactions;
					}
				});
		
		return result;
	}
	
	/** return success */
	public static void openLedgerFor(CommandSource viewer, User target) {
		VillagerShops.getAsyncScheduler().execute(()->{
			List<Transaction> raw;
			try {
				raw = getLedgerFor(target).get();
				List<Text> pages = new LinkedList<>();
				int entriesPerPage = (viewer instanceof Player)?2:10;
				int i = 0;
				Text.Builder page = Text.builder();
				for (Transaction t : raw) {
					if (i>=entriesPerPage) {
						i=0;
						pages.add(page.build());
						page = Text.builder();
					}
					if (i>0) page.append(Text.NEW_LINE);
					page.append(t.toText(viewer));
					i++;
				}
				if (i>0) pages.add(page.build());
//				VillagerShops.getSyncScheduler().execute(()->VillagerShops.l("Built %d pages", pages.size()));
				
				VillagerShops.getSyncScheduler().execute(()->{ //post back to main thread
					if (viewer instanceof Player) {
						((Player)viewer).sendBookView(BookView.builder()
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
//					VillagerShops.getSyncScheduler().execute(()->VillagerShops.l("Opened Ledger"));
				});
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		});
	}
}
