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
			// note: when setting yaw, always ensure it is NOT negative!!
			Vector endLoc = new Vector(display.location.getX() + (display.location.getYaw() == 270 ? 1 : (display.location.getYaw() == 90 ? -1 : 0)), display.location.getY() - Math.ceil((double) display.width / (double) display.mapIds.size()), display.location.getZ() + ((display.location.getYaw() == 0 || display.location.getYaw() == 360) ? 1 : (display.location.getYaw() == 180 ? -1 : 0)));
			if (numBetween(block.getX(), display.location.getX(), endLoc.getX()) && numBetween(block.getY(), display.location.getY(), endLoc.getY()) && numBetween(block.getZ(), display.location.getZ(), endLoc.getZ())) {
				Location plyrloc = player.getLocation();
				float yaw = plyrloc.getYaw();
				while (yaw < 0) {
					yaw += 360;
				}
				yaw = yaw % 360;
				if (((display.location.getYaw() == 0 || display.location.getYaw() == 360) && (yaw < 90 || yaw >= 270)) || (display.location.getYaw() == 90 && (yaw >= 0 && yaw < 180)) || (display.location.getYaw() == 180 && (yaw >= 90 && yaw < 270)) || (display.location.getYaw() == 270 && (yaw >= 180 && yaw < 360))) {
					//looking at screen from the correct angle
					BlockFace blockFace = getBlockFace(player, 5);
					if (blockFace == numberToBlockFace[(int) (((display.location.getYaw() / 90) + 2) % 4)]) {
						//correct block face
						Vector exactLoc = IntersectionUtils.getIntersection(player.getEyeLocation(), block, blockFace, 0);
						double y = 1.0 - (exactLoc.getY() - endLoc.getY()) / ((double) (display.mapIds.size() / display.width));
						double x = 0;
						if (display.location.getYaw() == 0 || display.location.getYaw() == 360) {
							//south -
							x = exactLoc.getX() - endLoc.getX();
						} else if (display.location.getYaw() == 90) {
							//west -
							x = exactLoc.getZ() - endLoc.getZ();
						} else if (display.location.getYaw() == 180) {
							//north +
							x = exactLoc.getX() - display.location.getX();
						} else if (display.location.getYaw() == 270) {
							//east +
							x = exactLoc.getZ() - display.location.getZ();
						}
						x = x / (double) display.width;
						if (display.location.getYaw() == 0 || display.location.getYaw() == 90 || display.location.getYaw() == 360) {
							x = 1.0 - x;
						}

						y = Math.max(0, Math.min(1, y));
						x = Math.max(0, Math.min(1, x));



						player.sendMessage(x + ", " + y);


						/*
						int slot = player.getInventory().getHeldItemSlot();
						if (!doClick) {
							MakiDesktop.clickMouse(x, y, 0, false);
						} else if (slot == 6) {
							MakiDesktop.alwaysMoveMouse = !MakiDesktop.alwaysMoveMouse;
							player.sendMessage("No" + (MakiDesktop.alwaysMoveMouse ? "w" : " longer") + " controlling mouse.");
						} else if (slot == 7) {
							MakiDesktop.clickMouse(x, y, 0, false);
						} else if (slot == 8) {
							// do nothing
						} else {
							MakiDesktop.clickMouse(x, y, (slot % 3) + 1, slot == 3 || slot == 4 || slot == 5);
						}
						*/
						return true;
					}
				}
			}
		}
		return false;
	}
}