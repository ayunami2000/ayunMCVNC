package me.ayunami2000.ayunMCVNC;

import net.minecraft.server.v1_12_R1.PacketPlayOutMap;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

class FramePacketSender extends BukkitRunnable implements Listener {
	private long frameNumber = 0;
	private final Queue<byte[][]> frameBuffers;

	public FramePacketSender(Queue<byte[][]> frameBuffers) {
		this.frameBuffers = frameBuffers;
		Main.plugin.getServer().getPluginManager().registerEvents(this, Main.plugin);
	}

	@Override
	public void run() {
		byte[][] buffers = frameBuffers.poll();
		if (buffers == null) {
			return;
		}
		// todo: only send if within certain distance...?
		List<PacketPlayOutMap> packets = new ArrayList<>(DisplayInfo.screenParts.size());
		for (ScreenPart screenPart : DisplayInfo.screenParts) {
			byte[] buffer = buffers[screenPart.partId];
			if (buffer != null) {
				PacketPlayOutMap packet = getPacket(screenPart.mapId, buffer);
				if (!screenPart.modified) {
					packets.add(0, packet);
				} else {
					packets.add(packet);
				}
				screenPart.modified = true;
				screenPart.lastFrameBuffer = buffer;
			} else {
				screenPart.modified = false;
			}
		}

		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			sendToPlayer(onlinePlayer, packets);
		}

		if (frameNumber % 300 == 0) {
			byte[][] peek = frameBuffers.peek();
			if (peek != null) {
				frameBuffers.clear();
				frameBuffers.offer(peek);
			}
		}
		frameNumber++;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		//do i REALLY need this to be added to the task list? disabled for now...
		new BukkitRunnable() {
			@Override
			public void run() {
				List<PacketPlayOutMap> packets = new ArrayList<>();
				for (ScreenPart screenPart : DisplayInfo.screenParts) {
					if (screenPart.lastFrameBuffer != null) {
						//this SHOULD work but it doesn't lol
						packets.add(getPacket(screenPart.mapId, screenPart.lastFrameBuffer));
					}
				}
				sendToPlayer(event.getPlayer(), packets);
				//todo: maybe remove from task list once we get here?
			}
		}.runTaskLater(Main.plugin, 10);
		//MakiDesktop.tasks.add(task);
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
				mapId, (byte) 0, false, null,
				data, 0, 0, 0, 0);
		/*
		this.colors = new byte[16384];
		int i = (128 - short0) / 2;
		int j = (128 - short1) / 2;

		for(int k = 0; k < short1; ++k) {
			int l = k + j;
			if (l >= 0 || l < 128) {
				for(int i1 = 0; i1 < short0; ++i1) {
					int j1 = i1 + i;
					if (j1 >= 0 || j1 < 128) {
						this.colors[j1 + l * 128] = abyte[i1 + k * short0];
					}
				}
			}
		}
		*/
	}
}

