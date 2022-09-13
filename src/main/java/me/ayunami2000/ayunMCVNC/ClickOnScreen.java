package me.ayunami2000.ayunMCVNC;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

public class ClickOnScreen {
	static BlockFace[] numberToBlockFace = new BlockFace[]{BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST};

	private static boolean numBetween(double num, double val1, double val2) {
		double maxVal = Math.max(val1, val2);
		double minVal = Math.min(val1, val2);
		return num >= minVal && num <= maxVal;
	}

	public static BlockFace getBlockFace(Player player, int maxDistance) {
		return player.getLastTwoTargetBlocks(null, maxDistance).get(1).getFace(player.getLastTwoTargetBlocks(null, maxDistance).get(0));
	}

	public static boolean clickedOnBlock(Block block, Player player, boolean doClick) {
		Collection<DisplayInfo> displays = DisplayInfo.displays.values();
		for (DisplayInfo display : displays) {
			if (display.paused) continue;
			float dYaw = ImageManager.fixYaw(display.location.getYaw());
			System.out.println(display.location);
			Vector endLoc = new Vector(display.location.getX() + (dYaw == 0 ? display.width : (dYaw == 180 ? -display.width : 0)), display.location.getY() + 1 - Math.ceil(display.mapIds.size() / (double) display.width), display.location.getZ() + (dYaw == 270 ? display.width : (dYaw == 90 ? -display.width : 0)));
			System.out.println(endLoc);
			System.out.println(block.getLocation());
			if (numBetween(block.getX(), display.location.getX(), endLoc.getX()) && numBetween(block.getY(), display.location.getY(), endLoc.getY()) && numBetween(block.getZ(), display.location.getZ(), endLoc.getZ())) {
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
						Vector exactLoc = IntersectionUtils.getIntersection(player.getEyeLocation(), block, blockFace, 0);
						double y = 1.0 - (exactLoc.getY() - endLoc.getY()) / ((double) (display.mapIds.size() / display.width));
						double x = 0;
						if (dYaw == 0) {
							//south -
							x = exactLoc.getX() - endLoc.getX();
						} else if (dYaw == 90) {
							//west -
							x = exactLoc.getZ() - endLoc.getZ();
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



						player.sendMessage(x + ", " + y);


						int slot = player.getInventory().getHeldItemSlot();
						if (!doClick) {
							display.videoCapture.clickMouse(x, y, 0, false);
						} else if (slot == 6) {
							// MakiDesktop.alwaysMoveMouse = !MakiDesktop.alwaysMoveMouse;
							// player.sendMessage("No" + (MakiDesktop.alwaysMoveMouse ? "w" : " longer") + " controlling mouse.");
						} else if (slot == 7) {
							display.videoCapture.clickMouse(x, y, 0, false);
						} else if (slot == 8) {
							// do nothing
						} else {
							display.videoCapture.clickMouse(x, y, (slot % 3) + 1, slot == 3 || slot == 4 || slot == 5);
						}
						return true;
					}
				}
			}
		}
		return false;
	}
}