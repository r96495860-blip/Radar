package dev.radar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
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

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_MENU_KEY.consumeClick()) {
				client.setScreen(new TargetScreen(targetName));
			}
		});
	}

	static void setTargetName(String name) {
		targetName = name.trim();
	}

	public static boolean isTarget(Player player) {
		Minecraft client = Minecraft.getInstance();
		return client.player != null
			&& player != client.player
			&& !targetName.isBlank()
			&& player.getGameProfile().name().equalsIgnoreCase(targetName);
	}
}
