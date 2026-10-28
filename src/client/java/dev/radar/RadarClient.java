package dev.radar;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

	private static final Set<String> targetNames = new LinkedHashSet<>();
	private static final Map<String, Integer> targetColors = new LinkedHashMap<>();

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_MENU_KEY.consumeClick()) {
				client.setScreen(new TargetScreen(targetNames, targetColors));
			}
		});
	}

	static void setTargets(Set<String> names, Map<String, Integer> colors) {
		targetNames.clear();
		targetNames.addAll(names);
		targetColors.clear();
		targetColors.putAll(colors);
	}

	public static boolean isTarget(Player player) {
		Minecraft client = Minecraft.getInstance();
		return client.player != null
			&& player != client.player
			&& targetNames.stream().anyMatch(name -> player.getGameProfile().name().equalsIgnoreCase(name));
	}

	public static int getOutlineColor(Player player) {
		return targetColors.getOrDefault(colorKey(player.getGameProfile().name()), 0xFF8A24);
	}

	static String colorKey(String name) {
		return name.toLowerCase(Locale.ROOT);
	}
}
