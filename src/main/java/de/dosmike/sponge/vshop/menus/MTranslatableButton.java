package de.dosmike.sponge.vshop.menus;

import de.dosmike.sponge.megamenus.api.elements.IIcon;
import de.dosmike.sponge.megamenus.api.elements.concepts.IClickable;
import de.dosmike.sponge.megamenus.api.listener.OnClickListener;
import de.dosmike.sponge.megamenus.impl.RenderManager;
import de.dosmike.sponge.megamenus.impl.TextMenuRenderer;
import de.dosmike.sponge.megamenus.impl.elements.IElementImpl;
import de.dosmike.sponge.vshop.VillagerShops;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.property.SlotPos;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.util.LinkedList;
import java.util.List;

/**
 * This element acts like a button, it can be clicked at and performs an action.
 */
final public class MTranslatableButton extends IElementImpl implements IClickable<MTranslatableButton> {

    private IIcon defaultIcon = null;
    private OnClickListener<MTranslatableButton> clickListener = null;
    private String nameTranslation = "invalid";
    private List<String> loreLineTransaltions = new LinkedList<>();

    @Override
    public OnClickListener<MTranslatableButton> getOnClickListener() {
        return clickListener;
    }

    @Override
    public void fireClickEvent(Player viewer, int button, boolean shift) {
        if (clickListener != null)
            clickListener.onClick(this, viewer, button, shift);
    }

    @Override
    public void setOnClickListener(OnClickListener<MTranslatableButton> listener) {
        clickListener = listener;
    }

    @Override
    public IIcon getIcon(Player viewer) {
        return defaultIcon;
    }

    @Override
    public Text getName(Player viewer) {
        return VillagerShops.getTranslator().localText(nameTranslation)
                .resolve(viewer)
                .orElse(Text.of(nameTranslation));
    }

    @Override
    public List<Text> getLore(Player viewer) {
        List<Text> lore = new LinkedList<>();
        for (String s : loreLineTransaltions) {
            lore.add(Text.of(TextColors.GRAY,
                    VillagerShops.getTranslator().localText(s)
                            .resolve(viewer)
                            .orElse(Text.of(s))
            ));
        }
        return lore;
    }

    /**
     * set the icon for this element
     * @param icon the new {@link IIcon} to display
     */
    public void setIcon(IIcon icon) {
        defaultIcon = icon;
    }
    /**
     * set the translation for this elements name
     * @param nameTranslation the new localization value for this element
     */
    public void setName(String nameTranslation) {
        this.nameTranslation = nameTranslation;
    }
    /**
     * set the lore for this element by translation names
     * @param lore a list containing the localizations for each line in the item lore
     */
    public void setLore(List<String> lore) {
        loreLineTransaltions = new LinkedList<>(lore);
    }

    public MTranslatableButton() {}

    //Region builder
    public static class Builder {
        final MTranslatableButton element = new MTranslatableButton();
        private Builder() {
        }

        public Builder setPosition(SlotPos position) {
            element.setPosition(position);
            return this;
        }

        public Builder setIcon(IIcon icon) {
            element.defaultIcon = icon;
            return this;
        }
        public Builder setIcon(ItemStackSnapshot icon) {
            element.defaultIcon = IIcon.of(icon);
            return this;
        }
        public Builder setIcon(ItemStack icon) {
            element.defaultIcon = IIcon.of(icon);
            return this;
        }
        public Builder setIcon(ItemType icon) {
            element.defaultIcon = IIcon.of(icon);
            return this;
        }
        public Builder setName(String nameTranslation) {
            element.nameTranslation = nameTranslation;
            return this;
        }
        public Builder setLore(List<String> lore) {
            element.loreLineTransaltions.clear();
            element.loreLineTransaltions.addAll(lore);
            return this;
        }

        public Builder setOnClickListener(OnClickListener<MTranslatableButton> listener) {
            element.clickListener = listener;
            return this;
        }

        public MTranslatableButton build() {
            return element.copy();
        }
    }

    public static Builder builder() {
        return new Builder();
    }
    //endregion

    @Override
    public Text renderTUI(Player viewer) {
        IIcon icon = getIcon(viewer);
        List<Text> lore = getLore(viewer);
        Text display = getName(viewer);
        display = Text.builder()
                .append(display)
                .style(TextStyles.of(TextStyles.ITALIC, TextStyles.UNDERLINE))
                .build();
        if (lore.isEmpty()) {
            return Text.builder().append(display)
            .onClick(TextActions.executeCallback((src)-> RenderManager.getRenderFor((Player)src)
                    .filter(r->(r instanceof TextMenuRenderer))
                    .ifPresent(r->((TextMenuRenderer)r).delegateClickEvent(MTranslatableButton.this, (Player)src))))
            .build();
        } else {
            List<Text> sublore = lore.size()>1 ? lore.subList(1,lore.size()) : new LinkedList<>();
            return Text.builder().append(display).onHover(
                    icon != null
                    ? TextActions.showItem(ItemStack.builder().fromSnapshot(icon.render())
                            .add(Keys.DISPLAY_NAME, lore.get(0))
                            .add(Keys.ITEM_LORE, sublore)
                            .build().createSnapshot())
                    : TextActions.showText(Text.of(
                            Text.joinWith(Text.of(Text.NEW_LINE), lore)
                    ))
            ).onClick(TextActions.executeCallback((src)-> RenderManager.getRenderFor((Player)src)
                    .filter(r->(r instanceof TextMenuRenderer))
                    .ifPresent(r->((TextMenuRenderer)r).delegateClickEvent(MTranslatableButton.this, (Player)src))))
            .build();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public MTranslatableButton copy() {
        MTranslatableButton copy = new MTranslatableButton();
        copy.setPosition(getPosition());
        copy.setParent(getParent());
        copy.nameTranslation = nameTranslation;
        copy.defaultIcon = defaultIcon;
        copy.loreLineTransaltions = new LinkedList<>(loreLineTransaltions);
        copy.clickListener = clickListener;
        return copy;
    }
}
