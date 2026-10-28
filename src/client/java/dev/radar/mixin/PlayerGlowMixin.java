package dev.radar.mixin;

import dev.radar.RadarClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PlayerGlowMixin {
	@Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
	private void radar$forceTargetGlow(CallbackInfoReturnable<Boolean> callback) {
		if ((Object) this instanceof Player player && RadarClient.isTarget(player)) {
			callback.setReturnValue(true);
		}
	}

	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void radar$setTargetOutlineColor(CallbackInfoReturnable<Integer> callback) {
		if ((Object) this instanceof Player player && RadarClient.isTarget(player)) {
			callback.setReturnValue(RadarClient.getOutlineColor(player));
		}
	}
}
