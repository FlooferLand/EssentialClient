package me.senseiwells.essentialclient.feature;

import me.senseiwells.essentialclient.rule.ClientRules;
import me.senseiwells.essentialclient.utils.misc.Events;
import me.senseiwells.essentialclient.utils.render.Texts;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class AFKRules {
	public static AFKRules INSTANCE = new AFKRules();

	private Vec3d prevPlayerLocation;

	private int ticks = 0;
	private double prevMouseX;
	private double prevMouseY;
	private boolean wasAfk = false;

	public void load() {
		Events.ON_DISCONNECT.register(client -> {
			this.wasAfk = false;
		});
		Events.ON_TICK_POST.register(client -> {
			ClientPlayerEntity playerEntity = client.player;
			int announceAfk = ClientRules.ANNOUNCE_AFK.getValue();
			int logout = ClientRules.AFK_LOGOUT.getValue();
			if (playerEntity == null || (announceAfk < 1 && logout < 200)) {
				return;
			}
			Vec3d playerLocation = playerEntity.getPos();
			double mouseX = client.mouse.getX();
			double mouseY = client.mouse.getX();
			if (playerLocation.equals(this.prevPlayerLocation) && mouseX == this.prevMouseX && mouseY == this.prevMouseY) {
				this.ticks++;
				if (this.ticks == announceAfk) {
					playerEntity.sendChatMessage(ClientRules.ANNOUNCE_AFK_MESSAGE.getValue());
				}
				if (logout >= 200 && this.ticks == logout) {
					playerEntity.networkHandler.onDisconnected(Texts.literal("You've been lazy! (AFK Logout)"));
				}
				return;
			}
			this.prevPlayerLocation = playerLocation;
			this.prevMouseX = mouseX;
			this.prevMouseY = mouseY;
			this.ticks = 0;
			if (this.wasAfk) {
				String message = ClientRules.ANNOUNCE_BACK_MESSAGE.getValue();
				if (!message.isBlank()) {
					playerEntity.sendChatMessage(message);
				}
				this.wasAfk = false;
			}
		});
	}
}
