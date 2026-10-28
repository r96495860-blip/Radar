package dev.radar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
	private static final int CHIP_WIDTH = 20;
	private static final int CHIP_GAP = 4;
	private static final int CHIP_HEIGHT = 13;
	private static final int[] OUTLINE_COLORS = {
		0xFF8A24,
		0xF04438,
		0xB35CFF,
		0x68D94A,
		0x55DDE0,
		0xFFE066,
		0xF0E6FF
	};
	private static final String[] COLOR_NAMES = {
		"screen.radar.color.pumpkin",
		"screen.radar.color.blood",
		"screen.radar.color.witch",
		"screen.radar.color.slime",
		"screen.radar.color.ghost",
		"screen.radar.color.moon",
		"screen.radar.color.spirit"
	};

	private final Set<String> pendingTargets;
	private final Map<String, Integer> pendingColors;
	private final List<String> onlinePlayers = new ArrayList<>();
	private EditBox searchField;
	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int listTop;
	private int listBottom;
	private int scrollOffset;
	private int activeColor;

	public TargetScreen(Set<String> selectedTargets, Map<String, Integer> selectedColors) {
		super(Component.translatable("screen.radar.title"));
		this.pendingTargets = new LinkedHashSet<>(selectedTargets);
		this.pendingColors = new LinkedHashMap<>(selectedColors);
		this.activeColor = selectedTargets.stream()
			.map(RadarClient::colorKey)
			.map(pendingColors::get)
			.filter(color -> color != null)
			.findFirst()
			.orElse(OUTLINE_COLORS[0]);
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
				pendingColors.clear();
				scrollOffset = 0;
			}
		).bounds(panelX + panelWidth - 88, panelY + 34, 72, 20).build());

		addRenderableWidget(Button.builder(
			Component.translatable("screen.radar.apply"),
			button -> {
				RadarClient.setTargets(pendingTargets, pendingColors);
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
			pendingColors.putIfAbsent(RadarClient.colorKey(name), activeColor);
		}
	}

	private void addSearchTarget() {
		String name = searchField.getValue().trim();
		if (!name.isEmpty() && !isSelected(name)) {
			pendingTargets.add(name);
			pendingColors.putIfAbsent(RadarClient.colorKey(name), activeColor);
		}
		searchField.setValue("");
		searchField.setFocused(true);
	}

	private void chooseColor(int color) {
		activeColor = color;
		for (String name : pendingTargets) {
			pendingColors.put(RadarClient.colorKey(name), color);
		}
	}

	private int colorFor(String name) {
		return pendingColors.getOrDefault(RadarClient.colorKey(name), activeColor);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		renderBackground(graphics, mouseX, mouseY, delta);

		graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF0181021);
		graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 3, 0xFFFF8A24);
		graphics.fill(panelX + 8, panelY + 7, panelX + panelWidth - 8, panelY + 8, 0xFF63305E);
		graphics.drawString(this.font, this.title, panelX + 16, panelY + 16, 0xFFFFB454);
		renderPumpkin(graphics);
		graphics.drawString(this.font, Component.translatable("screen.radar.subtitle"),
			panelX + 16, panelY + 36, 0xFFD4B9D2);
		graphics.drawString(this.font, Component.translatable("screen.radar.selected", pendingTargets.size()),
			panelX + 16, panelY + 91, 0xFFFFB454);
		renderColorPalette(graphics, mouseX, mouseY);

		super.render(graphics, mouseX, mouseY, delta);

		graphics.fill(panelX + 12, listTop - 5, panelX + panelWidth - 12, listBottom, 0xB00D0915);
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
						0xFF352039);
				}

				boolean selected = isSelected(name);
				int boxColor = selected ? 0xFFFF8A24 : 0xFF765F7A;
				graphics.fill(panelX + 22, rowY + 5, panelX + 34, rowY + 17, boxColor);
				if (selected) {
					graphics.drawString(this.font, "✓", panelX + 23, rowY + 4, 0xFFFFFFFF);
				}
				graphics.drawString(this.font, name, panelX + 42, rowY + 7, 0xFFF9EAF8);
				int swatchX = panelX + panelWidth - 38;
				int color = colorFor(name);
				graphics.fill(swatchX - 2, rowY + 3, swatchX + 14, rowY + 19, 0xFF100B18);
				graphics.fill(swatchX, rowY + 5, swatchX + 12, rowY + 17, 0xFF000000 | color);
				if (onlinePlayers.stream().noneMatch(online -> online.equalsIgnoreCase(name))) {
					graphics.drawString(this.font, Component.translatable("screen.radar.offline"),
						panelX + panelWidth - 88, rowY + 7, 0xFFB39BAF);
				}
			}
		}

		if (names.size() > visibleRows) {
			graphics.drawString(this.font,
				Component.translatable("screen.radar.scroll", scrollOffset + 1, names.size()),
				panelX + 16, listBottom + 5, 0xFF8C96A6);
		}
	}

	private void renderColorPalette(GuiGraphics graphics, int mouseX, int mouseY) {
		int chipY = panelY + 91;
		int startX = panelX + panelWidth - 170;
		graphics.drawString(this.font, Component.translatable("screen.radar.colors"),
			startX - 62, chipY + 3, 0xFFD4B9D2);
		for (int index = 0; index < OUTLINE_COLORS.length; index++) {
			int chipX = startX + index * (CHIP_WIDTH + CHIP_GAP);
			boolean active = activeColor == OUTLINE_COLORS[index];
			graphics.fill(chipX - 2, chipY - 2, chipX + CHIP_WIDTH + 2, chipY + CHIP_HEIGHT + 2,
				active ? 0xFFFFB454 : 0xFF54345B);
			graphics.fill(chipX, chipY, chipX + CHIP_WIDTH, chipY + CHIP_HEIGHT,
				0xFF000000 | OUTLINE_COLORS[index]);
			if (mouseX >= chipX - 2 && mouseX < chipX + CHIP_WIDTH + 2
				&& mouseY >= chipY - 2 && mouseY < chipY + CHIP_HEIGHT + 2) {
				graphics.drawString(this.font, Component.translatable(COLOR_NAMES[index]),
					mouseX + 8, mouseY + 8, 0xFFFFE6B8);
			}
		}
	}

	private void renderPumpkin(GuiGraphics graphics) {
		int x = panelX + panelWidth - 30;
		int y = panelY + 12;
		graphics.fill(x + 10, y, x + 17, y + 5, 0xFF72A34A);
		graphics.fill(x + 3, y + 5, x + 24, y + 19, 0xFFFF8A24);
		graphics.fill(x, y + 8, x + 27, y + 16, 0xFFFF8A24);
		graphics.fill(x + 7, y + 9, x + 11, y + 13, 0xFF32152B);
		graphics.fill(x + 16, y + 9, x + 20, y + 13, 0xFF32152B);
		graphics.fill(x + 9, y + 15, x + 18, y + 17, 0xFF32152B);
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		int chipY = panelY + 91;
		int startX = panelX + panelWidth - 170;
		if (event.button() == 0 && event.x() >= startX
			&& event.y() >= chipY - 2 && event.y() < chipY + CHIP_HEIGHT + 2) {
			for (int chip = 0; chip < OUTLINE_COLORS.length; chip++) {
				int chipX = startX + chip * (CHIP_WIDTH + CHIP_GAP);
				if (event.x() >= chipX - 2 && event.x() < chipX + CHIP_WIDTH + 2) {
					chooseColor(OUTLINE_COLORS[chip]);
					return true;
				}
			}
		}
		if (event.button() != 0 || event.x() < panelX + 12 || event.x() >= panelX + panelWidth - 12
			|| event.y() < listTop || event.y() >= listBottom) {
			return false;
		}

		int row = (int) (event.y() - listTop) / ROW_HEIGHT;
		List<String> names = visiblePlayers();
		int index = row + scrollOffset;
		if (index >= 0 && index < names.size()) {
			String name = names.get(index);
			if (event.x() >= panelX + panelWidth - 44) {
				int currentColor = colorFor(name);
				int colorIndex = 0;
				for (int i = 0; i < OUTLINE_COLORS.length; i++) {
					if (OUTLINE_COLORS[i] == currentColor) {
						colorIndex = i;
						break;
					}
				}
				pendingColors.put(RadarClient.colorKey(name),
					OUTLINE_COLORS[(colorIndex + 1) % OUTLINE_COLORS.length]);
				return true;
			}
			toggleTarget(name);
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
