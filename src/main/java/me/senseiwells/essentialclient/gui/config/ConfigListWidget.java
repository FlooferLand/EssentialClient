package me.senseiwells.essentialclient.gui.config;

import me.senseiwells.essentialclient.gui.RulesScreen;
import me.senseiwells.essentialclient.gui.entries.*;
import me.senseiwells.essentialclient.utils.interfaces.Rule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;

import java.util.Collection;
import java.util.Locale;

public class ConfigListWidget extends ElementListWidget<ConfigListWidget.Entry> {
	public static final int LENGTH = 136;

	public ConfigListWidget(RulesScreen rulesScreen, MinecraftClient client, String filter) {
		super(client, rulesScreen.width + 45, rulesScreen.height, 43, rulesScreen.height - 32, 20);
		this.reloadEntries(rulesScreen, filter);
	}

	public void reloadEntries(RulesScreen rulesScreen, String filter) {
		this.clearEntries();
		Collection<? extends Rule<?>> rules = rulesScreen.getRules();

		rules.forEach(rule -> {
			String ruleName = rule.getName();
			if (filter != null && !ruleName.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))) {
				return;
			}
			BaseListEntry<?> entry = switch (rule.getType()) {
				case BOOLEAN -> new BooleanListEntry((Rule.Bool) rule, this.client, rulesScreen);
				case CYCLE -> new CycleListEntry((Rule.Cycle) rule, this.client, rulesScreen);
				case INTEGER, DOUBLE -> new NumberListEntry(rule, this.client, rulesScreen);
				case STRING -> new StringListEntry(rule, this.client, rulesScreen);
				case SLIDER -> new SliderListEntry((Rule.Slider<?>) rule, this.client, rulesScreen);
				case LIST -> new ListListEntry((Rule.ListRule) rule, this.client, rulesScreen);
				default -> throw new IllegalStateException("Unexpected value: " + rule.getType());
			};
			this.addEntry(entry);
		});
	}

	public void updateAllEntriesOnClose() {
		this.children().forEach(Entry::updateEntryOnClose);
	}

	public void unFocusAll() {
		this.children().forEach(Entry::unFocus);
	}

	@Override
	protected int getScrollbarPositionX() {
		return this.width / 2 + this.getRowWidth() / 2 + 4;
	}

	@Override
	public int getRowWidth() {
		return 360;
	}

	public abstract static class Entry extends ElementListWidget.Entry<ConfigListWidget.Entry> {
		public void unFocus() { }

		public void updateEntryOnClose() { }
	}
}
