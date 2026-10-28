package dev.radar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public final class RadarClient implements ClientModInitializer {
	private static final KeyMapping OPEN_MENU_KEY = KeyBindingHelper.registerKeyBinding(
		new KeyMapping(
			"key.radar.open_menu",
			GLFW.GLFW_KEY_G,
			KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("radar", "main"))
		)
	);

	private static String targetName = "";
	private static AbstractClientPlayer highlightedPlayer;
	private static boolean wasGlowing;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_MENU_KEY.consumeClick()) {
				client.setScreen(new TargetScreen(targetName));
			}

			updateHighlight(client);
		});
	}

	static void setTargetName(String name) {
		targetName = name.trim();
	}

	static String getTargetName() {
		return targetName;
	}

	private static void updateHighlight(Minecraft client) {
		AbstractClientPlayer target = null;
		if (client.level != null && !targetName.isEmpty()) {
			target = client.level.players().stream()
				.filter(player -> player != client.player)
				.filter(player -> player.getGameProfile().name().equalsIgnoreCase(targetName))
				.findFirst()
				.orElse(null);
		}

		if (target == highlightedPlayer) {
			if (target != null) {
				target.setGlowingTag(true);
			}
			return;
		}

		clearHighlight();
		if (target != null) {
			wasGlowing = target.hasGlowingTag();
			target.setGlowingTag(true);
			highlightedPlayer = target;
		}
	}

	private static void clearHighlight() {
		if (highlightedPlayer != null) {
			highlightedPlayer.setGlowingTag(wasGlowing);
			highlightedPlayer = null;
		}
	}
}
