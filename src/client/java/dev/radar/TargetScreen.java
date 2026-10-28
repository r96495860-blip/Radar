package dev.radar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TargetScreen extends Screen {
	private static final int PANEL_WIDTH = 340;
	private static final int PANEL_HEIGHT = 310;
	private static final int ROW_HEIGHT = 22;

	private final Set<String> pendingTargets;
	private final List<String> onlinePlayers = new ArrayList<>();
	private EditBox searchField;
	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int listTop;
	private int listBottom;
	private int scrollOffset;

	public TargetScreen(Set<String> selectedTargets) {
		super(Component.translatable("screen.radar.title"));
		this.pendingTargets = new LinkedHashSet<>(selectedTargets);
	}

	@Override
	protected void init() {
		panelWidth = Math.min(PANEL_WIDTH, this.width - 24);
		panelHeight = Math.min(PANEL_HEIGHT, this.height - 20);
		panelX = (this.width - panelWidth) / 2;
		panelY = (this.height - panelHeight) / 2;
		listTop = panelY + 112;
		listBottom = panelY + panelHeight - 48;
		scrollOffset = 0;
		refreshOnlinePlayers();

		searchField = new EditBox(
			this.font,
			panelX + 16,
			panelY + 68,
			panelWidth - 112,
			20,
			Component.translatable("screen.radar.search")
		);
		searchField.setMaxLength(16);
		searchField.setHint(Component.translatable("screen.radar.search_hint"));
		searchField.setFocused(true);
		addRenderableWidget(searchField);

		addRenderableWidget(Button.builder(
			Component.translatable("screen.radar.add"),
			button -> addSearchTarget()
		).bounds(panelX + panelWidth - 88, panelY + 68, 72, 20).build());

		addRenderableWidget(Button.builder(
			Component.translatable("screen.radar.clear"),
			button -> {
				pendingTargets.clear();
				scrollOffset = 0;
			}
		).bounds(panelX + panelWidth - 88, panelY + 34, 72, 20).build());

		addRenderableWidget(Button.builder(
			Component.translatable("screen.radar.apply"),
			button -> {
				RadarClient.setTargets(pendingTargets);
				onClose();
			}
		).bounds(panelX + 16, panelY + panelHeight - 34, (panelWidth - 40) / 2, 20).build());

		addRenderableWidget(Button.builder(
			Component.translatable("screen.radar.cancel"),
			button -> onClose()
		).bounds(panelX + 24 + (panelWidth - 40) / 2, panelY + panelHeight - 34, (panelWidth - 40) / 2, 20).build());
	}

	private void refreshOnlinePlayers() {
		onlinePlayers.clear();
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.player != null) {
			client.level.players().stream()
				.filter(player -> player != client.player)
				.map(player -> player.getGameProfile().name())
				.distinct()
				.forEach(onlinePlayers::add);
		}
		onlinePlayers.sort(String.CASE_INSENSITIVE_ORDER);
	}

	private List<String> visiblePlayers() {
		List<String> names = new ArrayList<>(onlinePlayers);
		for (String selected : pendingTargets) {
			if (names.stream().noneMatch(name -> name.equalsIgnoreCase(selected))) {
				names.add(selected);
			}
		}
		names.sort(Comparator.comparing((String name) -> !isSelected(name))
			.thenComparing(String.CASE_INSENSITIVE_ORDER));

		String query = searchField == null ? "" : searchField.getValue().trim();
		if (!query.isEmpty()) {
			names.removeIf(name -> !name.toLowerCase(java.util.Locale.ROOT)
				.contains(query.toLowerCase(java.util.Locale.ROOT)));
		}
		return names;
	}

	private boolean isSelected(String name) {
		return pendingTargets.stream().anyMatch(target -> target.equalsIgnoreCase(name));
	}

	private void toggleTarget(String name) {
		if (isSelected(name)) {
			pendingTargets.removeIf(target -> target.equalsIgnoreCase(name));
		} else {
			pendingTargets.add(name);
		}
	}

	private void addSearchTarget() {
		String name = searchField.getValue().trim();
		if (!name.isEmpty() && !isSelected(name)) {
			pendingTargets.add(name);
		}
		searchField.setValue("");
		searchField.setFocused(true);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		renderBackground(graphics, mouseX, mouseY, delta);

		graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF0181B24);
		graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 3, 0xFFFF4545);
		graphics.drawString(this.font, this.title, panelX + 16, panelY + 16, 0xFFFFFFFF);
		graphics.drawString(this.font, Component.translatable("screen.radar.subtitle"),
			panelX + 16, panelY + 36, 0xFFADB5C4);
		graphics.drawString(this.font, Component.translatable("screen.radar.selected", pendingTargets.size()),
			panelX + 16, panelY + 90, 0xFFFF7777);

		super.render(graphics, mouseX, mouseY, delta);

		graphics.fill(panelX + 12, listTop - 5, panelX + panelWidth - 12, listBottom, 0xA0101219);
		List<String> names = visiblePlayers();
		int visibleRows = Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
		if (names.isEmpty()) {
			graphics.drawCenteredString(this.font,
				Component.translatable("screen.radar.empty"),
				panelX + panelWidth / 2, listTop + 12, 0xFF9AA3B2);
		} else {
			scrollOffset = Math.min(scrollOffset, Math.max(0, names.size() - visibleRows));
			for (int row = 0; row < visibleRows && row + scrollOffset < names.size(); row++) {
				String name = names.get(row + scrollOffset);
				int rowY = listTop + row * ROW_HEIGHT;
				boolean hovered = mouseX >= panelX + 12 && mouseX < panelX + panelWidth - 12
					&& mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
				if (hovered) {
					graphics.fill(panelX + 14, rowY, panelX + panelWidth - 14, rowY + ROW_HEIGHT,
						0xFF292E3A);
				}

				boolean selected = isSelected(name);
				int boxColor = selected ? 0xFFFF4545 : 0xFF596170;
				graphics.fill(panelX + 22, rowY + 5, panelX + 34, rowY + 17, boxColor);
				if (selected) {
					graphics.drawString(this.font, "✓", panelX + 23, rowY + 4, 0xFFFFFFFF);
				}
				graphics.drawString(this.font, name, panelX + 42, rowY + 7, 0xFFFFFFFF);
				if (onlinePlayers.stream().noneMatch(online -> online.equalsIgnoreCase(name))) {
					graphics.drawString(this.font, Component.translatable("screen.radar.offline"),
						panelX + panelWidth - 78, rowY + 7, 0xFF8C96A6);
				}
			}
		}

		if (names.size() > visibleRows) {
			graphics.drawString(this.font,
				Component.translatable("screen.radar.scroll", scrollOffset + 1, names.size()),
				panelX + 16, listBottom + 5, 0xFF8C96A6);
		}
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (event.button() != 0 || event.x() < panelX + 12 || event.x() >= panelX + panelWidth - 12
			|| event.y() < listTop || event.y() >= listBottom) {
			return false;
		}

		int row = (int) (event.y() - listTop) / ROW_HEIGHT;
		List<String> names = visiblePlayers();
		int index = row + scrollOffset;
		if (index >= 0 && index < names.size()) {
			toggleTarget(names.get(index));
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (mouseX >= panelX + 12 && mouseX < panelX + panelWidth - 12
			&& mouseY >= listTop && mouseY < listBottom) {
			int visibleRows = Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
			int maxOffset = Math.max(0, visiblePlayers().size() - visibleRows);
			scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset - (int) Math.signum(scrollY) * 3));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
