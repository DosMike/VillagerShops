package de.dosmike.sponge.vshop.menus;

import de.dosmike.sponge.megamenus.api.MenuRenderer;
import de.dosmike.sponge.megamenus.api.elements.IIcon;
import de.dosmike.sponge.megamenus.api.elements.concepts.IClickable;
import de.dosmike.sponge.megamenus.api.elements.concepts.IValueChangeable;
import de.dosmike.sponge.megamenus.api.listener.OnChangeListener;
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

import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A slightly reduced localized spinner that uses index in the change event
 */
final public class MSpinnerIndex extends IElementImpl implements IClickable<MSpinnerIndex>, IValueChangeable<Integer, MSpinnerIndex> {

    private int index=0;
    private OnClickListener<MSpinnerIndex> clickListener = null;
    private OnChangeListener<Integer, MSpinnerIndex> changeListener = null;
    private String nameKey = "unnamed";
    private List<IIcon> defaultIcons = new LinkedList<>();
    private List<String> valueKeys = new LinkedList<>();

    @Override
    public void fireClickEvent(Player viewer, int button, boolean shift) {
        Integer oldValue = getValue();
        if (button==MouseEvent.BUTTON1) {
            int i = getSelectedIndex();
            setSelectedIndex(i >= getMaximumIndex() ? 0 : (i + 1));
            invalidate(viewer);
        } else if (button==MouseEvent.BUTTON2) {
            int i = getSelectedIndex();
            setSelectedIndex(i <= 0 ? getMaximumIndex() : (i - 1));
            invalidate(viewer);
        }
        if (clickListener != null) {
            clickListener.onClick(this, viewer, button, shift);
        }
        fireChangeListener(viewer, oldValue, getValue());
    }
    @Override
    public void fireChangeListener(Player viewer, Integer oldValue, Integer newValue) {
        if (changeListener!=null)
            changeListener.onValueChange(oldValue, newValue, this, viewer);
    }
    private OnClickListener<MSpinnerIndex> internalClickListener = (e, v, b, s) -> {
        Integer oldValue = MSpinnerIndex.this.getValue();
        if (b == MouseEvent.BUTTON1) {
            int i = MSpinnerIndex.this.getSelectedIndex();
            MSpinnerIndex.this.setSelectedIndex(i >= MSpinnerIndex.this.getMaximumIndex() ? 0 : (i + 1));
            RenderManager.getRenderFor(v).ifPresent(MenuRenderer::invalidate);
        } else if (b == MouseEvent.BUTTON2) {
            int i = MSpinnerIndex.this.getSelectedIndex();
            MSpinnerIndex.this.setSelectedIndex(i <= 0 ? MSpinnerIndex.this.getMaximumIndex() : (i - 1));
            RenderManager.getRenderFor(v).ifPresent(MenuRenderer::invalidate);
        }
        if (clickListener != null) {
            clickListener.onClick(e, v, b, s);
        }
        if (changeListener != null)
            changeListener.onValueChange(oldValue, MSpinnerIndex.this.getValue(), MSpinnerIndex.this, v);
    };

    /**
     * The currently selected index in this spinner as offset to the first value
     * @return the current value
     */
    public int getSelectedIndex() {
        return index;
    }

    /**
     * @return the maximum index as amount of values - 1
     */
    public int getMaximumIndex() {
        return defaultIcons.size()-1;
    }

    /**
     * Updates the spinners current index without invoking events
     * @param value the new index to display
     */
    public void setSelectedIndex(int value) {
        if (value < 0 || value >= defaultIcons.size())
            throw new IllegalArgumentException("Cyclic value out of range (0.."+ defaultIcons.size());
        index = value;
    }

    /**
     * @return the {@link Text} value for the currently selected index
     */
    public Integer getValue() {
        return getSelectedIndex();
    }
    /**
     * Peeks the next {@link Text} value in the cycle
     * @return the value that will be displayed after the current one
     */
    public Integer getNextValue() {
        int i = getSelectedIndex()+1;
        return i>getMaximumIndex()?0:i;
    }
    /**
     * Peeks the previous {@link Text} value in the cycle
     * @return the value that was displayed prior to the current one
     */
    public Integer getPreviousValue() {
        int i = getSelectedIndex()-1;
        return i<0?getMaximumIndex():i;
    }

    @Override
    public OnClickListener<MSpinnerIndex> getOnClickListener() {
        return internalClickListener;
    }

    @Override
    public OnChangeListener<Integer, MSpinnerIndex> getOnChangeListener() {
        return changeListener;
    }

    @Override
    public void setOnClickListener(OnClickListener<MSpinnerIndex> listener) {
        clickListener = listener;
    }

    @Override
    public IIcon getIcon(Player viewer) {
        return defaultIcons.get(index);
    }

    @Override
    public Text getName(Player viewer) {
        return VillagerShops.getTranslator()
                .localText(nameKey)
                .resolve(viewer)
                .orElse(Text.of(nameKey));
    }

    /**
     * The default implementation highlights the line with the same position ans the selected value
     * if the number of lines in this lore equals the number of possible values. Otherwise the set lore
     * is returned unmodified.
     * If you do not want this to style your lore overwrite with <code>return defaultValues;</code>
     */
    @Override
    public List<Text> getLore(Player viewer) {
        List<Text> coloredCopy = new LinkedList<>();
        for (int i = 0; i< valueKeys.size(); i++) {
            String line = VillagerShops.getTranslator()
                    .local(valueKeys.get(i))
                    .resolve(viewer)
                    .orElse(valueKeys.get(i));
            coloredCopy.add(Text.of(index == i
                            ? TextColors.YELLOW
                            : TextColors.DARK_GRAY,
                    line));
        }
        return coloredCopy;
    }

    @Override
    public void setOnChangeListener(OnChangeListener<Integer, MSpinnerIndex> listener) {
        changeListener = listener;
    }

    public MSpinnerIndex() {}

    //Region builder
    public static class Builder {
        MSpinnerIndex element = new MSpinnerIndex();
        private Builder() {
        }

        public Builder setPosition(SlotPos position) {
            element.setPosition(position);
            return this;
        }

        public Builder addValue(IIcon icon, String localization) {
            element.defaultIcons.add(icon);
            element.valueKeys.add(localization);
            return this;
        }
        public Builder addValue(ItemStackSnapshot icon, String localization) {
            element.defaultIcons.add(IIcon.of(icon));
            element.valueKeys.add(localization);
            return this;
        }
        public Builder addValue(ItemStack icon, String localization) {
            element.defaultIcons.add(IIcon.of(icon));
            element.valueKeys.add(localization);
            return this;
        }
        public Builder addValue(ItemType icon, String localization) {
            element.defaultIcons.add(IIcon.of(icon));
            element.valueKeys.add(localization);
            return this;
        }
        public Builder clearValues() {
            element.defaultIcons.clear();
            element.valueKeys.clear();
            return this;
        }
        public Builder setName(String localization) {
            element.nameKey = localization;
            return this;
        }
        public Builder setOnClickListener(OnClickListener<MSpinnerIndex> listener) {
            element.clickListener = listener;
            return this;
        }
        public Builder setOnChangeListener(OnChangeListener<Integer, MSpinnerIndex> listener) {
            element.changeListener = listener;
            return this;
        }

        public MSpinnerIndex build() {
            MSpinnerIndex copy = element.copy();
            return copy;
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
                .append(Text.of("► ",display))
                .style(TextStyles.of(TextStyles.ITALIC))
                .build();
        if (lore.isEmpty()) {
            return display;
        } else {
            List<Text> sublore = lore.size()>1 ? lore.subList(1,lore.size()) : Collections.EMPTY_LIST;
            return Text.builder().append(display).onHover(
                    icon != null
                        ? TextActions.showItem(ItemStack.builder().fromSnapshot(icon.render())
                            .add(Keys.DISPLAY_NAME, lore.get(0))
                            .add(Keys.ITEM_LORE, sublore)
                            .build().createSnapshot())
                        : TextActions.showText(Text.of(
                            Text.joinWith(Text.of(Text.NEW_LINE), getLore(viewer))
                    ))
            ).onClick(TextActions.executeCallback((src)->{
                RenderManager.getRenderFor((Player)src)
                        .filter(r->(r instanceof TextMenuRenderer))
                        .ifPresent(r->((TextMenuRenderer)r).delegateClickEvent(MSpinnerIndex.this, (Player)src));
            }))
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public MSpinnerIndex copy() {
        MSpinnerIndex copy = new MSpinnerIndex();
        copy.defaultIcons.addAll(defaultIcons);
        copy.setPosition(getPosition());
        copy.setParent(getParent());
        copy.nameKey = nameKey;
        copy.valueKeys = new LinkedList<>(valueKeys);
        copy.clickListener = clickListener;
        copy.changeListener = changeListener;
        return copy;
    }
}
