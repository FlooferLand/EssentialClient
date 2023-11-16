package me.senseiwells.essentialclient.mixins.chunkDebug;

import me.senseiwells.essentialclient.feature.chunkdebug.ChunkGrid;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
	@Shadow
	private int scaledWidth;
	@Shadow
	private int scaledHeight;

	@Inject(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/hud/InGameHud;renderCrosshair(Lnet/minecraft/client/gui/DrawContext;)V",
			shift = At.Shift.AFTER
		)
	)
	private void afterCrossHairRender(
		DrawContext context,
		float tickDelta,
		CallbackInfo ci
	) {
		if (ChunkGrid.instance != null) {
			ChunkGrid.instance.renderMinimap(this.scaledWidth, this.scaledHeight);
		}
	}
}
