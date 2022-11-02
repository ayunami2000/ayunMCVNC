package me.ayunami2000.ayunMCVNC;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.google.common.collect.EvictingQueue;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

class FramePacketSender extends BukkitRunnable {
	private long frameNumber = 0;
	public static final Queue<FrameItem> frameBuffers = EvictingQueue.create(4500);

	public FramePacketSender() {
	}

	private static class PacketItem {
		public DisplayInfo display;
		public PacketContainer packet;

		public PacketItem(DisplayInfo display, PacketContainer packet) {
			this.display = display;
			this.packet = packet;
		}
	}

	private Boolean hasIsCancelled = null;

	public boolean isCancelledSafe() {
		if (hasIsCancelled == null) {
			try {
				boolean cancelled = isCancelled();
				hasIsCancelled = true;
				return cancelled;
			} catch (NoSuchMethodError e) {
				hasIsCancelled = false;
				return false;
			}
		}
		if (hasIsCancelled.booleanValue()) {
			return isCancelled();
		} else {
			return false;
		}
	}

	@Override
	public void run() {
		for (int batch = 0; !isCancelledSafe() && batch < 5; batch++) {
			FrameItem frameItem = frameBuffers.poll();
			if (frameItem == null) {
				continue;
			}
			if (frameItem.display.altDisplay) {
				int[] pixels = frameItem.altFrameBuffer;
				List<ArmorStand> altDisplayPixels = new ArrayList<>(new ArrayList<>(frameItem.display.location.getWorld().getEntitiesByClass(ArmorStand.class)).stream().filter(armorStand -> armorStand.hasMetadata("mcvnc-alt_display")).sorted(Comparator.comparingInt(as -> as.getMetadata("mcvnc-alt_display").get(0).asInt())).collect(Collectors.toList()));
				for (int i = 0; i < altDisplayPixels.size(); i++) {
					ArmorStand armorStand = altDisplayPixels.get(i);
					ItemStack oldItem = armorStand.getHelmet();
					if (oldItem == null || oldItem.getType() != Material.LEATHER_HELMET) {
						oldItem = new ItemStack(Material.LEATHER_HELMET);
					}
					ItemMeta oldMeta = oldItem.getItemMeta();
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
						PacketContainer packet = getPacket(mapId, buffer);
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
							try {
								Main.plugin.protocolManager.sendServerPacket(player, packet.packet);
							} catch (InvocationTargetException e) {
								e.printStackTrace();
							}
						}).start();
					} else {
						try {
							Main.plugin.protocolManager.sendServerPacket(player, packet.packet);
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	private PacketContainer getPacket(int mapId, byte[] data) {
		if (data == null) {
			throw new NullPointerException("data is null");
		}
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.MAP);
		StructureModifier<Object> packetModifier = packet.getModifier();
		packetModifier.writeDefaults();
		StructureModifier<Integer> packetIntegers = packet.getIntegers();
		if (packetModifier.size() > 5) {
			packetIntegers.write(1, 0).write(2, 0).write(3, 128).write(4, 128);
			packet.getByteArrays().write(0, data);
		} else {
			try {
				int lastArg = packetModifier.size() - 1;
				packetModifier.write(lastArg, packetModifier.getField(lastArg).getType().getConstructor(int.class, int.class, int.class, int.class, byte[].class).newInstance(0, 0, 128, 128, data));
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException |
					 NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
		packetIntegers.write(0, mapId);
		packet.getBytes().write(0, (byte) 0);
		StructureModifier<Boolean> packetBooleans = packet.getBooleans();
		if (packetBooleans.size() > 0) {
			packetBooleans.write(0, false);
		}
		return packet;
	}
}

