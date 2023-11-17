package me.senseiwells.essentialclient.feature.chunkdebug;

import com.mojang.blaze3d.systems.RenderSystem;
import me.senseiwells.essentialclient.EssentialClient;
import me.senseiwells.essentialclient.utils.EssentialUtils;
import me.senseiwells.essentialclient.utils.render.ChildScreen;
import me.senseiwells.essentialclient.utils.render.RenderHelper;
import me.senseiwells.essentialclient.utils.render.Texts;
import me.senseiwells.essentialclient.utils.render.WidgetHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.Text;

public class ChunkDebugScreen extends ChildScreen {
	public static final int
		HEADER_HEIGHT = 30,
		FOOTER_ROW_HEIGHT = 20,
		FOOTER_ROW_PADDING = 5,
		FOOTER_ROW_COUNT = 2,
		FOOTER_HEIGHT = FOOTER_ROW_HEIGHT * FOOTER_ROW_COUNT + FOOTER_ROW_PADDING * (FOOTER_ROW_COUNT + 1);

	private final MinecraftClient client = EssentialUtils.getClient();
	private NumberFieldWidget xPositionBox;
	private NumberFieldWidget zPositionBox;
	private boolean canClick = false;

	public ChunkDebugScreen(Screen parent) {
		super(Texts.CHUNK_SCREEN, parent);
	}

	@Override
	public void init() {
		if (ChunkGrid.instance == null) {
			ChunkGrid.instance = new ChunkGrid(this.client, this.width, this.height);
		}
		EssentialClient.CHUNK_NET_HANDLER.requestChunkData(ChunkGrid.instance.getDimension());
		int buttonWidth = (this.width - FOOTER_ROW_PADDING * 4) / 3;
		int buttonHeight = this.height - FOOTER_ROW_HEIGHT * 3 + FOOTER_ROW_PADDING * 2;
		Text dimensionText = ChunkGrid.instance.getPrettyDimension();
		ButtonWidget dimensionButton = this.addDrawableChild(WidgetHelper.newButton(FOOTER_ROW_PADDING, buttonHeight, buttonWidth, FOOTER_ROW_HEIGHT, dimensionText, button -> {
			ChunkGrid.instance.cycleDimension();
			button.setMessage(ChunkGrid.instance.getPrettyDimension());
			EssentialClient.CHUNK_NET_HANDLER.requestChunkData(ChunkGrid.instance.getDimension());
		}));
		this.addDrawableChild(WidgetHelper.newButton(buttonWidth + FOOTER_ROW_PADDING * 2, buttonHeight, buttonWidth, FOOTER_ROW_HEIGHT, Texts.RETURN_TO_PLAYER, button -> {
			if (this.client.player != null) {
				ChunkGrid.instance.setDimension(this.client.player.getEntityWorld());
				dimensionButton.setMessage(ChunkGrid.instance.getPrettyDimension());
				int chunkX = this.client.player.getChunkPos().x;
				int chunkZ = this.client.player.getChunkPos().z;
				ChunkGrid.instance.setCentre(chunkX, chunkZ);
				this.xPositionBox.setText(String.valueOf(chunkX));
				this.zPositionBox.setText(String.valueOf(chunkZ));
				EssentialClient.CHUNK_NET_HANDLER.requestChunkData(ChunkGrid.instance.getDimension());
			}
		}));
		Text initialMinimapText = ChunkGrid.instance.getMinimapMode().prettyName;
		this.addDrawableChild(WidgetHelper.newButton(buttonWidth * 2 + FOOTER_ROW_PADDING * 3, buttonHeight, buttonWidth, FOOTER_ROW_HEIGHT, initialMinimapText, button -> {
			ChunkGrid.instance.cycleMinimap();
			button.setMessage(ChunkGrid.instance.getMinimapMode().prettyName);
		}));
		buttonHeight = this.height - FOOTER_ROW_HEIGHT * 2 + FOOTER_ROW_PADDING * 3;
		this.xPositionBox = new NumberFieldWidget(this.textRenderer, FOOTER_ROW_PADDING + 28, buttonHeight, buttonWidth - 30, 20, Texts.X);
		this.xPositionBox.setInitialValue(ChunkGrid.instance.getCentreX());
		this.zPositionBox = new NumberFieldWidget(this.textRenderer, buttonWidth + FOOTER_ROW_PADDING * 2 + 28, buttonHeight, buttonWidth - 30, 20, Texts.Z);
		this.zPositionBox.setInitialValue(ChunkGrid.instance.getCentreZ());
		this.addDrawableChild(WidgetHelper.newButton(buttonWidth * 2 + FOOTER_ROW_PADDING * 3, buttonHeight, buttonWidth, FOOTER_ROW_HEIGHT, Texts.REFRESH, button -> {
			if (hasControlDown()) {
				ChunkHandler.clearAllChunks();
				EssentialClient.CHUNK_NET_HANDLER.requestServerRefresh();
			} else {
				ChunkHandler.clearAllChunks();
				EssentialClient.CHUNK_NET_HANDLER.requestChunkData(ChunkGrid.instance.getDimension());
			}
		}));
		this.addDrawableChild(WidgetHelper.newButton(5, 5, 90, 20, Texts.CHUNK_CLUSTER_SCREEN, button -> {
			this.client.setScreen(new ChunkClusterScreen(ChunkHandler.getChunkCluster(ChunkGrid.instance.getDimension()), this));
		}));

		this.addDrawableChild(this.xPositionBox);
		this.addDrawableChild(this.zPositionBox);
	}

	@Override
	public void resize(MinecraftClient client, int width, int height) {
		if (ChunkGrid.instance != null) {
			ChunkGrid.instance.onResize(width, height - HEADER_HEIGHT - FOOTER_HEIGHT);
		}
		super.resize(client, width, height);
	}

	@Override
	public void close() {
		this.removed();
		super.close();
	}

	@Override
	public void removed() {
		if (ChunkGrid.instance.getMinimapMode() == ChunkGrid.Minimap.NONE) {
			EssentialClient.CHUNK_NET_HANDLER.requestChunkData();
			ChunkHandler.clearAllChunks();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderInGameBackground(context);
		ChunkGrid.instance.render(0, HEADER_HEIGHT, this.width, this.height - HEADER_HEIGHT - FOOTER_HEIGHT, false);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		this.drawHeaderAndFooterGradient(tessellator, bufferBuilder);
		this.drawHeaderAndFooterTexture(tessellator, bufferBuilder);
		super.render(context, mouseX, mouseY, delta);

		if (ChunkGrid.instance.isPanning()) {
			this.xPositionBox.setText(String.valueOf(ChunkGrid.instance.getCentreX()));
			this.zPositionBox.setText(String.valueOf(ChunkGrid.instance.getCentreZ()));
		}

		RenderHelper.drawScaledText(context, Texts.CHUNK_SCREEN, this.width / 2, 12, 1.5F, true);
		if (ChunkGrid.instance.getSelectionText() != null) {
			RenderHelper.drawScaledText(context, ChunkGrid.instance.getSelectionText(), this.width / 2, HEADER_HEIGHT + 10, 1, true);
		}

		this.xPositionBox.render(context, mouseX, mouseY, delta);
		this.zPositionBox.render(context, mouseX, mouseY, delta);

		int textHeight = this.height - 20;
		int xOffset = FOOTER_ROW_PADDING + 10;
		int zOffset = this.xPositionBox.getWidth() + 50;

		RenderHelper.drawScaledText(context, Texts.X, xOffset, textHeight, 1.5F, false);
		RenderHelper.drawScaledText(context, Texts.Z, zOffset, textHeight, 1.5F, false);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

	}

	private void drawHeaderAndFooterGradient(Tessellator tessellator, BufferBuilder bufferBuilder) {
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		RenderHelper.startQuads(bufferBuilder, VertexFormats.POSITION_COLOR);

		// Header gradient
		bufferBuilder.vertex(0, HEADER_HEIGHT, 0).color(0, 0, 0, 255).next();
		bufferBuilder.vertex(0, HEADER_HEIGHT + 4, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(this.width, HEADER_HEIGHT + 4, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(this.width, HEADER_HEIGHT, 0).color(0, 0, 0, 255).next();

		// Footer gradient
		bufferBuilder.vertex(0, this.height - FOOTER_HEIGHT - 4, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(0, this.height - FOOTER_HEIGHT, 0).color(0, 0, 0, 255).next();
		bufferBuilder.vertex(this.width, this.height - FOOTER_HEIGHT, 0).color(0, 0, 0, 255).next();
		bufferBuilder.vertex(this.width, this.height - FOOTER_HEIGHT - 4, 0).color(0, 0, 0, 0).next();

		tessellator.draw();
	}

	private void drawHeaderAndFooterTexture(Tessellator tessellator, BufferBuilder bufferBuilder) {
		RenderHelper.setPositionTextureColourShader();
		RenderHelper.bindTexture(OPTIONS_BACKGROUND_TEXTURE);

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();

		RenderHelper.startQuads(bufferBuilder, VertexFormats.POSITION_TEXTURE_COLOR);

		// Header
		bufferBuilder.vertex(0, 0, 0).texture(0, 0).color(64, 64, 64, 255).next();
		bufferBuilder.vertex(0, HEADER_HEIGHT, 0).texture(0, HEADER_HEIGHT / 32f).color(64, 64, 64, 255).next();
		bufferBuilder.vertex(this.width, HEADER_HEIGHT, 0).texture(this.width / 32f, HEADER_HEIGHT / 32f).color(64, 64, 64, 255).next();
		bufferBuilder.vertex(this.width, 0, 0).texture(this.width / 32f, 0).color(64, 64, 64, 255).next();

		// Footer
		bufferBuilder.vertex(0, this.height - FOOTER_HEIGHT, 0).texture(0, (this.height - FOOTER_HEIGHT) / 32f).color(64, 64, 64, 255).next();
		bufferBuilder.vertex(0, this.height, 0).texture(0, this.height / 32f).color(64, 64, 64, 255).next();
		bufferBuilder.vertex(this.width, this.height, 0).texture(this.width / 32f, this.height / 32f).color(64, 64, 64, 255).next();
		bufferBuilder.vertex(this.width, this.height - FOOTER_HEIGHT, 0).texture(this.width / 32f, (this.height - FOOTER_HEIGHT) / 32f).color(64, 64, 64, 255).next();

		tessellator.draw();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		return ChunkGrid.instance.onScroll(mouseX, mouseY, horizontalAmount + verticalAmount);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		this.canClick = true;
		ChunkGrid.instance.onClicked(mouseX, mouseY, button);
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (this.canClick) {
			ChunkGrid.instance.onRelease(mouseX, mouseY, button);
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		ChunkGrid.instance.onDragged(mouseX, mouseY, button);
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	private class NumberFieldWidget extends TextFieldWidget {
		private int lastValidValue;

		private NumberFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
			super(textRenderer, x, y, width, height, text);
		}

		private void setInitialValue(int value) {
			this.lastValidValue = value;
			this.setText(String.valueOf(value));
		}

		private int getValue() {
			return this.lastValidValue;
		}

		@Override
		public void setFocused(boolean focused) {
			if (this.isFocused() && !focused) {
				try {
					int newValue = Integer.parseInt(this.getText());
					if (this.lastValidValue != newValue) {
						this.lastValidValue = newValue;
						ChunkGrid.instance.setCentre(
							ChunkDebugScreen.this.xPositionBox.getValue(),
							ChunkDebugScreen.this.zPositionBox.getValue()
						);
					}
				} catch (NumberFormatException e) {
					this.setText(String.valueOf(this.lastValidValue));
				}
			}
			super.setFocused(focused);
		}
	}
}
