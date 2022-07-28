package me.senseiwells.essentialclient.mixins.clientScript;

import me.senseiwells.arucas.utils.impl.ArucasList;
import me.senseiwells.arucas.values.NumberValue;
import me.senseiwells.arucas.values.StringValue;
import me.senseiwells.essentialclient.clientscript.events.MinecraftScriptEvents;
import me.senseiwells.essentialclient.clientscript.values.BlockValue;
import me.senseiwells.essentialclient.clientscript.values.ItemStackValue;
import me.senseiwells.essentialclient.clientscript.values.PlayerValue;
import me.senseiwells.essentialclient.clientscript.values.TextValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Optional;
import java.util.UUID;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
	@Shadow
	private ClientWorld world;

	@Final
	@Shadow
	private MinecraftClient client;

	@Shadow private DynamicRegistryManager.Immutable registryManager;

	@Inject(method = "onHealthUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER))
	private void onHealthUpdate(HealthUpdateS2CPacket packet, CallbackInfo ci) {
		MinecraftScriptEvents.ON_HEALTH_UPDATE.run(NumberValue.of(packet.getHealth()));
	}

	@Inject(method = "onBlockUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER))
	private void onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci) {
		MinecraftScriptEvents.ON_BLOCK_UPDATE.run(new BlockValue(packet.getState(), packet.getPos()));
	}

	@Inject(method = "onEntityStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;showFloatingItem(Lnet/minecraft/item/ItemStack;)V"))
	private void onTotem(CallbackInfo ci) {
		MinecraftScriptEvents.ON_TOTEM.run();
	}

	@Inject(method = "onGameStateChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;setGameMode(Lnet/minecraft/world/GameMode;)V"))
	private void onGamemodeChange(GameStateChangeS2CPacket packet, CallbackInfo ci) {
		MinecraftScriptEvents.ON_GAMEMODE_CHANGE.run(StringValue.of(GameMode.byId((int) packet.getValue()).getName()));
	}

	@Inject(method = "onItemPickupAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER))
	private void onPickUp(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
		Entity entity = this.world.getEntityById(packet.getEntityId());
		LivingEntity livingEntity = (LivingEntity) this.world.getEntityById(packet.getCollectorEntityId());
		if (entity instanceof ItemEntity itemEntity && this.client.player != null && this.client.player.equals(livingEntity)) {
			MinecraftScriptEvents.ON_PICK_UP_ITEM.run(new ItemStackValue(itemEntity.getStack().copy()));
		}
	}

	@Inject(method = "onPlayerRespawn", at = @At("TAIL"))
	private void onPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
		MinecraftScriptEvents.ON_RESPAWN.run(new PlayerValue(this.client.player));
	}

	@Inject(method = "onGameMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER), cancellable = true)
	private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
		if (this.handleMessage(Util.NIL_UUID, packet.content().getString(), packet.overlay() ? "overlay" : "system")) {
			ci.cancel();
		}
	}

	@Inject(method = "onChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/message/MessageHandler;onChatMessage(Lnet/minecraft/network/message/SignedMessage;Lnet/minecraft/network/message/MessageType$Parameters;)V"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
	private void onChatMessage(ChatMessageS2CPacket packet, CallbackInfo ci, Optional<MessageType.Parameters> parameters) {
		MessageType messageType = parameters.get().type();
		Identifier typeId = this.registryManager.get(Registry.MESSAGE_TYPE_KEY).getId(messageType);
		String type = typeId == null ? "unknown" : typeId.getPath();
		if (this.handleMessage(packet.message().signedHeader().sender(), packet.message().getContent().getString(), type)) {
			ci.cancel();
		}
	}

	@Inject(method = "onPlayerList", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER))
	public void onPlayerList(PlayerListS2CPacket packet, CallbackInfo info) {
		switch (packet.getAction()) {
			case ADD_PLAYER -> packet.getEntries().forEach(entry -> MinecraftScriptEvents.ON_PLAYER_JOIN.run(StringValue.of(entry.getProfile().getName())));
			case REMOVE_PLAYER -> packet.getEntries().forEach(entry -> MinecraftScriptEvents.ON_PLAYER_LEAVE.run(StringValue.of(entry.getProfile().getName())));
		}
	}

	@Inject(method = "onDeathMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getEntityById(I)Lnet/minecraft/entity/Entity;", shift = At.Shift.BEFORE))
	private void onDeath(DeathMessageS2CPacket packet, CallbackInfo ci) {
		Entity entity = this.world.getEntityById(packet.getEntityId());
		if (entity == this.client.player) {
			Entity killer = this.world.getEntityById(packet.getKillerId());
			MinecraftScriptEvents.ON_DEATH.run(c -> ArucasList.arrayListOf(c.convertValue(killer), new TextValue(packet.getMessage().copy())));
		}
	}

	@Inject(method = "onEntitySpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;addEntity(ILnet/minecraft/entity/Entity;)V", shift = At.Shift.AFTER))
	private void onEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
		Entity entity = this.world.getEntityById(packet.getId());
		if (entity instanceof LivingEntity) {
			MinecraftScriptEvents.ON_MOB_SPAWN.run(c -> ArucasList.arrayListOf(c.convertValue(entity)));
			return;
		}
		if (entity != null) {
			MinecraftScriptEvents.ON_ENTITY_SPAWN.run(c -> ArucasList.arrayListOf(c.convertValue(entity)));
		}
	}

	@Inject(method = "method_37472", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;removeEntity(ILnet/minecraft/entity/Entity$RemovalReason;)V", shift = At.Shift.BEFORE))
	private void onEntityRemoved(int entityId, CallbackInfo ci) {
		Entity entity = this.world.getEntityById(entityId);
		MinecraftScriptEvents.ON_ENTITY_REMOVED.run(c -> ArucasList.arrayListOf(c.convertValue(entity)));
	}

	@Unique
	private boolean handleMessage(UUID uuid, String content, String messageType) {
		return MinecraftScriptEvents.ON_RECEIVE_MESSAGE.run(
			StringValue.of(uuid.toString()),
			StringValue.of(content),
			StringValue.of(messageType)
		);
	}
}
