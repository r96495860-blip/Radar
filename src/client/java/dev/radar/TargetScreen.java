package dev.radar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TargetScreen extends Screen {
	private final String initialName;
	private EditBox nameField;

	public TargetScreen(String initialName) {
		super(Component.translatable("screen.radar.title"));
		this.initialName = initialName;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int fieldY = this.height / 2 - 10;

		nameField = new EditBox(
			this.font,
			centerX - 100,
			fieldY,
			200,
			20,
			Component.translatable("screen.radar.player_name")
		);
		nameField.setMaxLength(16);
		nameField.setValue(initialName);
		nameField.setFocused(true);
		addRenderableWidget(nameField);

		addRenderableWidget(Button.builder(
			Component.translatable("screen.radar.apply"),
			button -> {
				RadarClient.setTargetName(nameField.getValue());
				onClose();
			}
		).bounds(centerX - 100, fieldY + 28, 96, 20).build());

		addRenderableWidget(Button.builder(
			Component.translatable("screen.radar.cancel"),
			button -> onClose()
		).bounds(centerX + 4, fieldY + 28, 96, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 42, 0xFFFFFF);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
