package me.senseiwells.essentialclient.feature.chunkdebug;

import me.senseiwells.essentialclient.EssentialClient;
import me.senseiwells.essentialclient.rule.ClientRules;
import me.senseiwells.essentialclient.utils.misc.Events;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChunkHandler {
	private static final Map<String, Set<ChunkData>> CHUNK_DATA_MAP = new HashMap<>();

	static {
		Events.ON_DISCONNECT.register(v -> {
			EssentialClient.onDisconnect();
			ChunkHandler.clearAllChunks();
			ChunkGrid.instance = null;
		});
	}

	public synchronized static ChunkData[] getChunks(String world) {
		Set<ChunkData> chunkDataSet = CHUNK_DATA_MAP.get(world);
		if (chunkDataSet == null) {
			return new ChunkData[0];
		}
		return chunkDataSet.toArray(ChunkData[]::new);
	}

	public synchronized static void clearAllChunks() {
		CHUNK_DATA_MAP.forEach((s, chunkData) -> chunkData.clear());
	}

	public synchronized static void deserializeAndProcess(String world, long[] chunkPositions, byte[] levelTypes, byte[] statusTypes, byte[] ticketTypes) {
		int size = chunkPositions.length;
		if (size != levelTypes.length || size != ticketTypes.length || size != statusTypes.length) {
			EssentialClient.LOGGER.error("Chunk debug received bad packet!");
			return;
		}

		CHUNK_DATA_MAP.putIfAbsent(world, new HashSet<>());
		Set<ChunkData> chunkDataSet = CHUNK_DATA_MAP.get(world);

		for (int i = 0; i < size; i++) {
			ChunkPos chunkPos = new ChunkPos(chunkPositions[i]);
			ChunkType chunkType = ChunkType.decodeChunkType(levelTypes[i]);
			TicketType ticketType = TicketType.decodeTicketType(ticketTypes[i]);
			ChunkStatus status = ChunkStatus.decodeChunkStatus(statusTypes[i]);
			ChunkData chunkData = new ChunkData(chunkPos, chunkType, status, ticketType);
			chunkDataSet.remove(chunkData);
			if (chunkType != ChunkType.UNLOADED || ClientRules.CHUNK_DEBUG_SHOW_UNLOADED_CHUNKS.getValue()) {
				chunkDataSet.add(chunkData);
			}
		}
	}

	public static class ChunkData {
		private final ChunkPos chunkPos;
		private final ChunkType chunkType;
		private final ChunkStatus chunkStatus;
		private final TicketType ticketType;

		public ChunkData(ChunkPos chunkPos, ChunkType chunkType, ChunkStatus chunkStatus, TicketType ticketType) {
			this.chunkPos = chunkPos;
			this.chunkType = chunkType;
			this.chunkStatus = chunkStatus;
			this.ticketType = ticketType;
		}

		public ChunkData(int posX, int posZ, ChunkType chunkType, ChunkStatus chunkStatus, TicketType ticketType) {
			this(new ChunkPos(posX, posZ), chunkType, chunkStatus, ticketType);
		}

		public int getPosX() {
			return this.chunkPos.x;
		}

		public int getPosZ() {
			return this.chunkPos.z;
		}

		public ChunkType getChunkType() {
			return this.chunkType;
		}

		public ChunkStatus getChunkStatus() {
			return this.chunkStatus;
		}

		public boolean hasTicketType() {
			return this.ticketType != null;
		}

		public TicketType getTicketType() {
			return this.ticketType;
		}

		public int getProminentColour() {
			return Colourable.getHighestPriority(this.chunkType, this.chunkStatus, this.ticketType).getColour();
		}

		@Override
		public int hashCode() {
			return this.chunkPos.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof ChunkData otherChunk) {
				return this.chunkPos.equals(otherChunk.chunkPos);
			}
			return false;
		}
	}
}
