package me.ayunami2000.ayunMCVNC;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

public class ClickOnScreen {
	public static BlockFace[] numberToBlockFace = new BlockFace[]{BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST};

	public static boolean numBetween(double num, double val1, double val2) {
		double maxVal = Math.max(val1, val2);
		double minVal = Math.min(val1, val2);
		return num >= minVal && num <= maxVal;
	}

	public static BlockFace getBlockFace(Player player, int maxDistance) {
		List<Block> targetBlocks = player.getLastTwoTargetBlocks(null, maxDistance);
		return targetBlocks.get(1).getFace(targetBlocks.get(0));
	}

	public static boolean clickedOnBlock(Block block, Player player, boolean doClick) {
		Collection<DisplayInfo> displays = DisplayInfo.displays.values();
		for (DisplayInfo display : displays) {
			if (display.paused) continue;
			if (!display.mouse) continue;
			if (!(display.videoCapture.videoCapture instanceof VideoCaptureVnc)) continue;
			float dYaw = 90F * (Math.round(display.location.getYaw() / 90F) % 4);
			if (numBetween(block.getX(), display.location.getX(), display.locEnd.getX()) && numBetween(block.getY(), display.location.getY(), display.locEnd.getY()) && numBetween(block.getZ(), display.location.getZ(), display.locEnd.getZ())) {
				Location plyrloc = player.getLocation();
				float yaw = plyrloc.getYaw();
				while (yaw < 0) {
					yaw += 360;
				}
				yaw = yaw % 360;
				if ((dYaw == 0 && (yaw < 90 || yaw >= 270)) || (dYaw == 90 && (yaw >= 0 && yaw < 180)) || (dYaw == 180 && (yaw >= 90 && yaw < 270)) || (dYaw == 270 && (yaw >= 180 && yaw < 360))) {
					//looking at screen from the correct angle
					BlockFace blockFace = getBlockFace(player, 5);
					if (blockFace == numberToBlockFace[(int) (((display.location.getYaw() / 90) + 2) % 4)]) {
						//correct block face
						Vector exactLoc = IntersectionUtils.getIntersection(player.getEyeLocation(), block, blockFace, 0.0625);
						double y = 1.0 - (exactLoc.getY() - display.locEnd.getY()) / ((double) (display.mapIds.size() / display.width));
						double x = 0;
						if (dYaw == 0) {
							//south -
							x = exactLoc.getX() - display.locEnd.getX();
						} else if (dYaw == 90) {
							//west -
							x = exactLoc.getZ() - display.locEnd.getZ();
						} else if (dYaw == 180) {
							//north +
							x = exactLoc.getX() - display.location.getX();
						} else if (dYaw == 270) {
							//east +
							x = exactLoc.getZ() - display.location.getZ();
						}
						x = x / (double) display.width;
						if (dYaw == 0 || dYaw == 90) {
							x = 1.0 - x;
						}

						y = Math.max(0, Math.min(1, y));
						x = Math.max(0, Math.min(1, x));

						int slot = player.getInventory().getHeldItemSlot();
						if (slot == 8) {
							// do nothing
						} else if (slot == 7 ? doClick : (slot == 6 || !doClick)) {
							display.videoCapture.clickMouse(x, y, 0, false);
						} else if (slot < 6) {
							display.videoCapture.clickMouse(x, y, (slot % 3) + 1, slot == 3 || slot == 4 || slot == 5);
						}
						if (!player.hasMetadata("lookingAtScreen")) {
							sendActionBar(player, "ayunMCVNC: Current tool: " + Main.plugin.slotTexts.get(slot));
						}
						player.setMetadata("lookingAtScreen", new FixedMetadataValue(Main.plugin, true));
						return true;
					}
				}
			}
		}
		if (player.hasMetadata("lookingAtScreen")) {
			player.removeMetadata("lookingAtScreen", Main.plugin);
		}
		return false;
	}

	private static Boolean canActionBar = null;

	public static void sendActionBar(Player player, String text) {
		if (canActionBar == null) {
			try {
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
				canActionBar = true;
			} catch (NoSuchMethodError e) {
				canActionBar = false;
			}
			return;
		}
		if (canActionBar.booleanValue()) {
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
		} else {
			PacketContainer packet = new PacketContainer(PacketType.Play.Server.CHAT);
			packet.getChatComponents().write(0, WrappedChatComponent.fromText(text));
			packet.getBytes().write(0, (byte) 2);
			try {
				Main.plugin.protocolManager.sendServerPacket(player, packet);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
}