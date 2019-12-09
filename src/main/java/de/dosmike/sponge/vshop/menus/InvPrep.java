package de.dosmike.sponge.vshop.menus;

import de.dosmike.sponge.langswitch.LocalizedText;
import de.dosmike.sponge.megamenus.MegaMenus;
import de.dosmike.sponge.megamenus.api.IMenu;
import de.dosmike.sponge.megamenus.api.MenuRenderer;
import de.dosmike.sponge.megamenus.api.elements.IIcon;
import de.dosmike.sponge.megamenus.api.elements.concepts.IElement;
import de.dosmike.sponge.megamenus.api.listener.OnRenderStateListener;
import de.dosmike.sponge.megamenus.api.state.StateObject;
import de.dosmike.sponge.megamenus.impl.BoundMenuImpl;
import de.dosmike.sponge.megamenus.impl.GuiRenderer;
import de.dosmike.sponge.megamenus.impl.RenderManager;
import de.dosmike.sponge.megamenus.impl.util.MenuUtil;
import de.dosmike.sponge.vshop.ConfigSettings;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.shops.InteractionHandler;
import de.dosmike.sponge.vshop.shops.NPCguard;
import de.dosmike.sponge.vshop.shops.StockItem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.SlotPos;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.*;
import java.util.stream.Collectors;

public class InvPrep {

    public enum QuantityValues {
        FULL(1f), HALF(0.5f), QUARTER(0.25f), SINGLE(0f);
        /** not-throw string parser with FULL fallback */
        public static QuantityValues fromString(String s) {
            for (QuantityValues v : values()) {
                if (v.name().equalsIgnoreCase(s)) return v;
            }
            return FULL;
        }
        public static QuantityValues getNth(int n) {
            return values()[n];
        }
        private final float multiplier;
        QuantityValues(float stackSizeMultiplier) {
            multiplier = stackSizeMultiplier;
        }
        public int getStackSize(ItemType type) {
            return Math.max(1, (int)Math.ceil(type.getMaxStackQuantity()*multiplier));
        }
    };
    public static final String MENU_REMOVEMODE = "removemode";
    public static final String MENU_REMOVESET = "removeindices";

    List<StockItem> items = new LinkedList<>();

    public void addItem(StockItem si) {
        items.add(si);
        VillagerShops.getInstance().markNpcsDirty();
        updateMenu(true);
    }

    public void removeIndex(int i) {
        items.remove(i);
        VillagerShops.getInstance().markNpcsDirty();
        updateMenu(true);
    }

    public void setItem(int index, StockItem element) {
        items.set(index, element);
        VillagerShops.getInstance().markNpcsDirty();
        updateMenu(true);
    }

    public StockItem getItem(int index) {
        return items.get(index);
    }
    public List<StockItem> getAllItems() {
        return items;
    }
    public void setAllItems(List<StockItem> items) {
        this.items = new LinkedList<>(items);
    }

    public int size() {
        return items.size();
    }

    private IMenu menu = MegaMenus.createMenu();
    public IMenu getMenu() {
        return menu;
    }

    public void updateMenu(boolean full) {
        if (full) {
            for (int i = menu.pages(); i>0; i--) menu.clearPage(i);
            int p=1, y=0, x=0;
            boolean itemsOnLastPage = false;
            for (int i = 0; i < items.size(); i++) {
                StockItem item = items.get(i);
                itemsOnLastPage = true;
                if (item.getBuyPrice() != null ||
                    item.getSellPrice() != null) {
                    MShop button = new MShop(item, i);
                    button.setPosition(new SlotPos(x,y));
                    menu.add(p, button);
                }
                if (++x > 8) {
                    x = 0;
                    if (++y > 4) {
                        y = 0;
                        p++;
                        itemsOnLastPage = false;
                    }
                }
            }
            for (int i = menu.pages(); i>0; i--) {
                if (menu.getPageElements(i).isEmpty())
                    menu.removePage(i);
            }
            if (!itemsOnLastPage) p--;
            //add quantity option toggle
            int bottomY = Math.max(2, Math.min((int)Math.ceil(size()/9.0)+1, 6))-1;


            for (int i = 1; i <= p; i++) {
                MSpinnerIndex qcpy = spnQuantity.copy();
                qcpy.setPosition(new SlotPos(8, bottomY));
                menu.add(i, qcpy);

                //this button needs to be hidden (in bound renderer) then not admin/owner
                MTranslatableButton bcpy = bnRemoveMode.copy();
                bcpy.setPosition(new SlotPos(0, bottomY));
                menu.add(i, bcpy);
            }
        }
        menu.invalidate();
    }

    public void updateStock(Inventory container) {
        for (StockItem item : items) item.updateStock(container);
        updateMenu(false);
    }

    MSpinnerIndex spnQuantity = MSpinnerIndex.builder()
            .setName("shop.quantity.name")
            .addValue(IIcon.of(ItemStack.of(ItemTypes.IRON_BLOCK, 64)), "shop.quantity.items.full")
            .addValue(IIcon.of(ItemStack.of(ItemTypes.IRON_INGOT, 32)), "shop.quantity.items.half")
            .addValue(IIcon.of(ItemStack.of(ItemTypes.IRON_INGOT, 16)), "shop.quantity.items.quarter")
            .addValue(IIcon.of(ItemStack.of(ItemTypes.IRON_NUGGET, 1)), "shop.quantity.items.single")
            .setOnChangeListener((oldValue, newValue, element, viewer) -> {
                element.getParent()
                        .getPlayerState(viewer).set(
                        MShop.MENU_SHOP_QUANTITY,
                        QuantityValues.getNth(newValue));
                //update button for all pages
                for (int page = 1; page <= element.getParent().pages(); page++) {
                    Optional<IElement> el = MenuUtil.getElementAt(element.getParent(), page, element.getPosition());
                    el.ifPresent(e->{
                        if (e instanceof MSpinnerIndex) {
                            ((MSpinnerIndex) e).setSelectedIndex(newValue);
                        }
                    });
                }
            })
            .build();
    MTranslatableButton bnRemoveMode = MTranslatableButton.builder()
            .setName("shop.removemode.name")
            .setLore(Arrays.asList("shop.removemode.lore1", "shop.removemode.lore2"))
            .setOnClickListener((element, player, button, shift) -> {
                StateObject menuState = element.getParent().getPlayerState(player);
                IIcon newButtonIcon;
                boolean inRemoveMode = menuState.getBoolean(MENU_REMOVEMODE).orElse(false);
                if (inRemoveMode) {
                    HashSet<Integer> indices = (HashSet<Integer>) menuState.get(MENU_REMOVESET).orElse(new HashSet<Integer>());
                    menuState.remove(MENU_REMOVEMODE);
                    menuState.remove(MENU_REMOVESET);

                    newButtonIcon = IIcon.of(ItemTypes.BUCKET);
                    UUID shopID = Utilities.getOpenShopFor(player);
                    if (shopID == null) { //Error
                        VillagerShops.l("Could not find open shop");
                        RenderManager.getRenderFor(player).ifPresent(MenuRenderer::closeAll);
                        return;
                    }

                    if (!indices.isEmpty()) { //remove items
                        IMenu menu = (element.getParent() instanceof BoundMenuImpl)
                                ? ((BoundMenuImpl) element.getParent()).getBaseMenu()
                                : element.getParent();
                        RenderManager.getRenderFor(menu).forEach(MenuRenderer::closeAll);

                        TreeSet<Integer> sorted = new TreeSet<>(Comparator.reverseOrder());
                        sorted.addAll(indices);

                        VillagerShops.audit("%s", Utilities.toString(player) +
                                " deleted " + sorted.size() +
                                " items from shop " +
                                VillagerShops.getNPCfromShopUUID(shopID)
                                        .map(NPCguard::toString)
                                        .orElse("[" + shopID + "]") +
                                ": " + indices.stream()
                                        .map(i -> getItem(i).toString())
                                        .collect(Collectors.joining(", ", "[ ", " ]"))
                        );

                        Iterator<Integer> ii = sorted.iterator();
                        ii.forEachRemaining(this::removeIndex);

                        Task.builder().name("Reopen Shop").delayTicks(2).execute(()->{
                            VillagerShops.getNPCfromShopUUID(shopID).ifPresent(npcGuard -> {
                                InteractionHandler.clickEntity(player, npcGuard.getLe());
                            });
                        }).submit(VillagerShops.getInstance());

                        return;
                    }
                } else {
                    menuState.set(MENU_REMOVEMODE, true);
                    menuState.set(MENU_REMOVESET, new HashSet<Integer>());
                    newButtonIcon = IIcon.of(ItemTypes.LAVA_BUCKET);
                }
                //update button for all pages
                for (int page = 1; page <= element.getParent().pages(); page++) {
                    Optional<IElement> el = MenuUtil.getElementAt(element.getParent(), page, element.getPosition());
                    el.ifPresent(e->{
                        if (e instanceof MTranslatableButton) {
                            ((MTranslatableButton) e).setIcon(newButtonIcon);
                            e.invalidate();
                        }
                    });
                }
            })
            .setIcon(IIcon.of(ItemTypes.BUCKET))
            .build();

    public GuiRenderer createRenderer(Player source, @Nullable Text shopName, boolean administrativeInterface) {
        int idealHeight = Math.max(2, Math.min((int)Math.ceil(size()/9.0)+1, 6)); // 2 - 6 rows
        GuiRenderer renderer = (GuiRenderer) getMenu().createGuiRenderer(idealHeight, true);
        renderer.getMenu().setTitle(Text.of(TextColors.DARK_AQUA,
                ((LocalizedText)VillagerShops.getTranslator().localText("shop.title"))
                        .replace("%name%", Text.of(TextColors.RESET, shopName == null ? Text.EMPTY : shopName))
                        .setContextColor(TextColors.DARK_AQUA)
                        .resolve(source).orElse(Text.of("[vShop] ", shopName == null ? Text.EMPTY : shopName))));

        //relink player state value to the spinner
        IMenu minstance = renderer.getMenu();
        SlotPos posBQ = new SlotPos(8,idealHeight-1); //buy quantity spinner
        SlotPos posRM = new SlotPos(0,idealHeight-1); //remove button
        int qsi = minstance.getPlayerState(source)
                        .getOfClass(MShop.MENU_SHOP_QUANTITY, InvPrep.QuantityValues.class)
                        .orElse(ConfigSettings.getShopsDefaultStackSize()).ordinal();
        for (int i = 1; i <= minstance.pages(); i++) {
            MenuUtil.getElementAt(minstance, i, posBQ).ifPresent(e -> {
                if (e instanceof MSpinnerIndex) {
                    ((MSpinnerIndex) e).setSelectedIndex(qsi);
                    e.invalidate();
                }
            });
            if (!administrativeInterface) {
                minstance.remove(i, posRM);
            }
        }

        renderer.setRenderListener(new OnRenderStateListener() {
            @Override
            public boolean closed(MenuRenderer render, IMenu menu, Player viewer) {
                Utilities.actionUnstack.remove(viewer.getUniqueId());
                Utilities._openShops_remove(viewer);

                StateObject state = menu.getPlayerState(viewer);
                state.remove(InvPrep.MENU_REMOVEMODE);
                state.remove(InvPrep.MENU_REMOVESET);
                return false;
            }
        });
        return renderer;
    }

}
