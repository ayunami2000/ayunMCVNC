package me.ayunami2000.ayunMCVNC;

import com.google.common.collect.EvictingQueue;
import net.minecraft.server.v1_12_R1.PacketPlayOutMap;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

class FramePacketSender extends BukkitRunnable {
	private long frameNumber = 0;
	public static final Queue<FrameItem> frameBuffers = EvictingQueue.create(4500);

	public FramePacketSender() {
	}

	private static class PacketItem {
		public DisplayInfo display;
		public PacketPlayOutMap packet;

		public PacketItem(DisplayInfo display, PacketPlayOutMap packet) {
			this.display = display;
			this.packet = packet;
		}
	}

	@Override
	public void run() {
		for (int batch = 0; !isCancelled() && batch < 5; batch++) {
			FrameItem frameItem = frameBuffers.poll();
			if (frameItem == null) {
				continue;
			}
			if (frameItem.display.altDisplay) {
				int[] pixels = frameItem.altFrameBuffer;
				List<ArmorStand> altDisplayPixels = new ArrayList<>(new ArrayList<>(frameItem.display.location.getWorld().getEntitiesByClass(ArmorStand.class)).stream().filter(armorStand -> armorStand.hasMetadata("mcvnc-alt_display")).sorted(Comparator.comparingInt(as -> as.getMetadata("mcvnc-alt_display").get(0).asInt())).collect(Collectors.toList()));
				int heigg = 128 * frameItem.display.mapIds.size() / frameItem.display.width;
				for (int i = 0; i < altDisplayPixels.size(); i++) {
					ArmorStand armorStand = altDisplayPixels.get(i);
					// int currX = Math.max(0, Math.min(128 * frameItem.display.width - 1, (int) ((frameItem.display.location.getZ() - armorStand.getLocation().getZ()) * 128)));
					// int currY = Math.max(0, Math.min(heigg - 1, (int) ((frameItem.display.location.getY() - armorStand.getLocation().getY()) * 128)));
					// Location newLoc = armorStand.getLocation().clone();
					/*
					if (currY + 1 >= heigg) {
						newLoc.setY(frameItem.display.location.getY());
						armorStand.teleport(newLoc);
					} else {
						newLoc.subtract(0, 0.0078125, 0);
						armorStand.teleport(newLoc);
					}
					currY++;
					*/
					/*
					newLoc.setY(frameItem.display.location.getY() - (Math.random() * heigg / 128));
					armorStand.teleport(newLoc);
					*/
					// currY = Math.max(0, Math.min(heigg - 1, (int) ((frameItem.display.location.getY() - armorStand.getLocation().getY()) * 128)));
					ItemStack oldItem = armorStand.getHelmet();
					if (oldItem == null || oldItem.getType() != Material.LEATHER_HELMET) {
						oldItem = new ItemStack(Material.LEATHER_HELMET);
					}
					ItemMeta oldMeta = oldItem.getItemMeta();
					// int pixVal = pixels[(frameItem.display.width * 128 * currY) + currX];
					int pixVal = pixels[armorStand.getMetadata("mcvnc-alt_display").get(0).asInt()];
					((LeatherArmorMeta) oldMeta).setColor(Color.fromRGB(pixVal));
					oldItem.setItemMeta(oldMeta);
					altDisplayPixels.get(i).setHelmet(oldItem);
				}
			} else {
				byte[][] buffers = frameItem.frameBuffer;
				List<PacketItem> packets = new ArrayList<>(frameItem.display.mapIds.size());
				int numMaps = frameItem.display.mapIds.size();
				for (int i = 0; i < numMaps; i++) {
					byte[] buffer = buffers[i];
					int mapId = frameItem.display.mapIds.get(i);
					if (buffer != null) {
						PacketPlayOutMap packet = getPacket(mapId, buffer);
						boolean modified = DisplayInfo.screenPartModified.contains(mapId);
						if (!modified) {
							packets.add(0, new PacketItem(frameItem.display, packet));
						} else {
							packets.add(new PacketItem(frameItem.display, packet));
						}
						DisplayInfo.screenPartModified.add(mapId);
					} else {
						DisplayInfo.screenPartModified.remove(mapId);
					}
				}

				for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
					sendToPlayer(onlinePlayer, packets);
				}
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
	}

	private void sendToPlayer(Player player, List<PacketItem> packets) {
		CraftPlayer craftPlayer = (CraftPlayer) player;
		if (!player.hasPermission("ayunmcvnc.view")) return;
		for (PacketItem packet : packets) {
			if (packet != null && packet.packet != null) {
				if (DisplayInfo.getSorted(player, 4096, true, false).contains(packet.display)) {
					if (packet.display.audio > 0) {
						new Thread(() -> {
							try {
								Thread.sleep(packet.display.audio);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							craftPlayer.getHandle().playerConnection.networkManager.sendPacket(packet.packet);
						}).start();
					} else {
						craftPlayer.getHandle().playerConnection.networkManager.sendPacket(packet.packet);
					}
				}
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

