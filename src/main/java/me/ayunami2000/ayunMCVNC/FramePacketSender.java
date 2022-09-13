package me.ayunami2000.ayunMCVNC;

import com.google.common.collect.EvictingQueue;
import net.minecraft.server.v1_12_R1.PacketPlayOutMap;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;

class FramePacketSender extends BukkitRunnable {
	private long frameNumber = 0;
	public static final Queue<FrameItem> frameBuffers = EvictingQueue.create(4500);

	public FramePacketSender() {
	}

	@Override
	public void run() {
		FrameItem frameItem = frameBuffers.poll();
		if (frameItem == null) {
			return;
		}
		byte[][] buffers = frameItem.frameBuffer;
		// todo: only send if within certain distance...?
		List<PacketPlayOutMap> packets = new ArrayList<>(frameItem.display.mapIds.size());
		int numMaps = frameItem.display.mapIds.size();
		for (int i = 0; i < numMaps; i++) {
			byte[] buffer = buffers[i];
			int mapId = frameItem.display.mapIds.get(i);
			if (buffer != null) {
				PacketPlayOutMap packet = getPacket(mapId, buffer);
				boolean modified = DisplayInfo.screenPartModified.contains(mapId);
				if (!modified) {
					packets.add(0, packet);
				} else {
					packets.add(packet);
				}
				DisplayInfo.screenPartModified.add(mapId);
			} else {
				DisplayInfo.screenPartModified.remove(mapId);
			}
		}
		// notice: below doesnt work for multiple displays... (bc of use of buffers[i])
		/*
		Collection<DisplayInfo> displays = DisplayInfo.displays.values();
		for (DisplayInfo displayInfo : displays) {
			for (int i = 0; i < displayInfo.mapIds.size(); i++) {
				byte[] buffer = buffers[i];
				int mapId = frameItem.display.mapIds.get(i);
				if (buffer != null) {
					PacketPlayOutMap packet = getPacket(mapId, buffer);
					boolean modified = DisplayInfo.screenPartModified.contains(mapId);
					if (!modified) {
						packets.add(0, packet);
					} else {
						packets.add(packet);
					}
					DisplayInfo.screenPartModified.add(mapId);
				} else {
					DisplayInfo.screenPartModified.remove(mapId);
				}
			}
		}
		*/

		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			sendToPlayer(onlinePlayer, packets);
		}

		if (frameNumber % 300 == 0) {
			FrameItem peek = frameBuffers.peek();
			if (peek != null) {
				frameBuffers.clear();
				frameBuffers.offer(peek);
			}
			frameNumber = 0;
		}
		frameNumber++;
	}

	private void sendToPlayer(Player player, List<PacketPlayOutMap> packets) {
		CraftPlayer craftPlayer = (CraftPlayer) player;
		for (PacketPlayOutMap packet : packets) {
			if (packet != null) {
				craftPlayer.getHandle().playerConnection.networkManager.sendPacket(packet);
			}
		}
	}

	private PacketPlayOutMap getPacket(int mapId, byte[] data) {
		if (data == null) {
			throw new NullPointerException("data is null");
		}
		return new PacketPlayOutMap(
				mapId, (byte) 0, false, new HashSet<>(),
				data, 0, 0, 128, 128);
	}
}

