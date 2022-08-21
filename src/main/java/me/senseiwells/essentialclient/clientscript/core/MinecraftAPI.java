package me.senseiwells.essentialclient.clientscript.core;

import com.mojang.brigadier.builder.ArgumentBuilder;
import me.senseiwells.arucas.api.ArucasAPI;
import me.senseiwells.arucas.values.StringValue;
import me.senseiwells.essentialclient.clientscript.extensions.*;
import me.senseiwells.essentialclient.clientscript.values.*;
import me.senseiwells.essentialclient.feature.keybinds.MultiKeyBind;
import me.senseiwells.essentialclient.utils.EssentialUtils;
import me.senseiwells.essentialclient.utils.render.FakeInventoryScreen;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffer;
import net.minecraft.world.biome.Biome;

public class MinecraftAPI {
	public static final String
		BIOME = "Biome",
		BOX_SHAPE = "BoxShape",
		CONFIG_HANDLER = "ConfigHandler",
		FAKE_BLOCK = "FakeBlock",
		FAKE_ENTITY = "FakeEntity",
		GAME_EVENT = "GameEvent",
		KEY_BIND = "KeyBind",
		LINE_SHAPE = "LineShape",
		SPHERE_SHAPE = "SphereShape",
		BLOCK = "Block",
		COMMAND_BUILDER = "CommandBuilder",
		CONFIG = "Config",
		ENTITY = "Entity",
		FAKE_SCREEN = "FakeScreen",
		ITEM_ENTITY = "ItemEntity",
		ITEM_STACK = "ItemStack",
		JSON = "Json",
		LIVING_ENTITY = "LivingEntity",
		MATERIAL = "Material",
		MERCHANT_SCREEN = "MerchantScreen",
		MINECRAFT_CLIENT = "MinecraftClient",
		MINECRAFT_TASK = "MinecraftTask",
		OTHER_PLAYER = "OtherPlayer",
		PLAYER = "Player",
		POS = "Pos",
		RECIPE = "Recipe",
		SCREEN = "Screen",
		TEXT = "Text",
		TRADE = "Trade",
		WORLD = "World";

	public static final String IMPORT_NAME = "Minecraft";

	@SuppressWarnings({"deprecation", "unchecked"})
	public static void addMinecraftAPI(ArucasAPI.Builder builder) {
		builder.addClassDefinitions(
			IMPORT_NAME,
			MinecraftClientValue.ArucasMinecraftClientMembers::new,
			CommandBuilderValue.CommandBuilderClass::new,
			PlayerValue.ArucasPlayerClass::new,
			EntityValue.ArucasEntityClass::new,
			OtherPlayerValue.ArucasAbstractPlayerClass::new,
			LivingEntityValue.ArucasLivingEntityClass::new,
			ItemEntityValue.ArucasItemEntityClass::new,
			BlockValue.ArucasBlockClass::new,
			ItemStackValue.ArucasItemStackClass::new,
			WorldValue.ArucasWorldClass::new,
			BiomeValue.ArucasBiomeClass::new,
			ScreenValue.ArucasScreenClass::new,
			FakeInventoryScreenValue.ArucasFakeInventoryScreenClass::new,
			MerchantScreenValue.ArucasMerchantScreenClass::new,
			TextValue.ArucasTextClass::new,
			MaterialValue.ArucasMaterialClass::new,
			PosValue.ArucasPosClass::new,
			RecipeValue.ArucasRecipeClass::new,
			TradeValue.ArucasTradeOfferClass::new,
			ConfigValue.ArucasConfigValue::new
		);
		builder.addWrappers(
			IMPORT_NAME,
			GameEventWrapper::new,
			BoxShapeWrapper::new,
			SphereShapeWrapper::new,
			LineShapeWrapper::new,
			FakeEntityWrapper::new,
			FakeBlockWrapper::new,
			KeyBindWrapper::new,
			ConfigHandlerWrapper::new
		);
		builder.addBuiltInExtension(new ArucasMinecraftExtension());

		builder.addConversion(MinecraftClient.class, (m, c) -> MinecraftClientValue.INSTANCE);
		builder.addConversion(ClientPlayerEntity.class, (p, c) -> new PlayerValue(p));
		builder.addConversion(OtherClientPlayerEntity.class, (p, c) -> new OtherPlayerValue(p));
		builder.addConversion(LivingEntity.class, (l, c) -> new LivingEntityValue<>(l));
		builder.addConversion(ItemEntity.class, (i, c) -> new ItemEntityValue(i));
		builder.addConversion(Entity.class, (e, c) -> EntityValue.of(e));
		builder.addConversion(Block.class, (b, c) -> new BlockValue(b.getDefaultState()));
		builder.addConversion(BlockState.class, (b, c) -> new BlockValue(b));
		builder.addConversion(Item.class, (i, c) -> new MaterialValue(i));
		builder.addConversion(ItemStack.class, (i, c) -> new ItemStackValue(i));
		builder.addConversion(ClientWorld.class, (w, c) -> new WorldValue(w));
		builder.addConversion(Biome.class, (b, c) -> new BiomeValue(b));
		builder.addConversion(Screen.class, (s, c) -> ScreenValue.of(s));
		builder.addConversion(FakeInventoryScreen.class, (s, c) -> new FakeInventoryScreenValue(s));
		builder.addConversion(MerchantScreen.class, (s, c) -> new MerchantScreenValue(s));
		builder.addConversion(MutableText.class, (t, c) -> new TextValue(t));
		builder.addConversion(Text.class, (t, c) -> new TextValue(t.copy()));
		builder.addConversion(Vec3d.class, (p, c) -> new PosValue(p));
		builder.addConversion(Vec3f.class, (p, c) -> new PosValue(new Vec3d(p)));
		builder.addConversion(Vec3i.class, (p, c) -> new PosValue(new Vec3d(p.getX(), p.getY(), p.getZ())));
		builder.addConversion(Recipe.class, (r, c) -> new RecipeValue(r));
		builder.addConversion(TradeOffer.class, (t, c) -> new TradeValue(t));
		builder.addConversion(ArgumentBuilder.class, (a, c) -> new CommandBuilderValue(a));
		builder.addConversion(MultiKeyBind.class, KeyBindWrapper::newKeyBindWrapper);

		builder.addConversion(ItemStackArgument.class, (i, c) -> EssentialUtils.throwAsRuntime(() -> new ItemStackValue(i.createStack(1, false))));
		builder.addConversion(BlockStateArgument.class, (b, c) -> new BlockValue(b.getBlockState()));
		builder.addConversion(Identifier.class, (i, c) -> StringValue.of(i.toString()));
		builder.addConversion(Enchantment.class, (e, c) -> {
			Identifier identifier = Registry.ENCHANTMENT.getId(e);
			return identifier == null ? null : StringValue.of(identifier.toString());
		});
	}
}
