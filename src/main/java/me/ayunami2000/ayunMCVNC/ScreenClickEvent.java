package me.ayunami2000.ayunMCVNC;

import io.netty.channel.Channel;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;

public class ScreenClickEvent implements Listener {
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof ItemFrame) {
			ItemFrame itemFrame = (ItemFrame) event.getRightClicked();
			ItemStack itemStack = itemFrame.getItem();
			if (itemStack == null || itemStack.getType() != Material.MAP) return;
			Collection<DisplayInfo> displays = DisplayInfo.displays.values();
			for (DisplayInfo display : displays) {
				if (!display.mapIds.contains((int) itemStack.getDurability())) continue;
				float dYaw = 90F * (Math.round(display.location.getYaw() / 90F) % 4);
				BlockFace blockFace = itemFrame.getAttachedFace();
				if (dYaw == 0) {
					if (!blockFace.equals(BlockFace.SOUTH)) continue;
				} else if (dYaw == 90) {
					if (!blockFace.equals(BlockFace.WEST)) continue;
				} else if (dYaw == 180) {
					if (!blockFace.equals(BlockFace.NORTH)) continue;
				} else if (dYaw == 270) {
					if (!blockFace.equals(BlockFace.EAST)) continue;
				}
				Block block = itemFrame.getLocation().clone().add(blockFace.getModX(), blockFace.getModY(), blockFace.getModZ()).getBlock();
				if (ClickOnScreen.numBetween(block.getX(), display.location.getX(), display.locEnd.getX()) && ClickOnScreen.numBetween(block.getY(), display.location.getY(), display.locEnd.getY()) && ClickOnScreen.numBetween(block.getZ	(), display.location.getZ(), display.locEnd.getZ())) {
					event.setCancelled(true);
					Player player = event.getPlayer();
					if (!player.hasPermission("ayunmcvnc.interact")) return;
					ClickOnScreen.clickedOnBlock(block, player, true);
					break;
				}
			}
		}
	}

	private Boolean hasGetHand = null;

	private boolean isMainHand(PlayerInteractEvent event) {
		if (hasGetHand == null) {
			try {
				boolean mainHand = event.getHand() == EquipmentSlot.HAND;
				hasGetHand = true;
				return mainHand;
			} catch (NoSuchMethodError e) {
				hasGetHand = false;
				return true;
			}
		}
		if (hasGetHand.booleanValue()) {
			return event.getHand() == EquipmentSlot.HAND;
		} else {
			return true;
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onRightClick(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (!player.hasPermission("ayunmcvnc.interact")) return;
		Action action = event.getAction();

		if ((action.equals(Action.RIGHT_CLICK_BLOCK)) && isMainHand(event)) {
			Block block = event.getClickedBlock();
			Block tblock = player.getTargetBlock(null, 5);
			Location bloc = block.getLocation();
			Location tloc = tblock.getLocation();
			if (bloc.getX() == tloc.getX() && bloc.getY() == tloc.getY() && bloc.getZ() == tloc.getZ()) {
				ClickOnScreen.clickedOnBlock(block, player, true);
			}
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (!player.hasPermission("ayunmcvnc.interact")) return;
		Location from = event.getFrom();
		Location to = event.getTo();
		if (from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch()) {
			Block tgtbl = player.getTargetBlock(null, 5);
			if (tgtbl != null) ClickOnScreen.clickedOnBlock(tgtbl, player, false);
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Channel channel = getKey(AudioServer.wsList, event.getPlayer().getName());
		if (channel != null) {
			channel.close();
		}
	}

	private <K, V> K getKey(Map<K, V> map, V value) {
		for (Map.Entry<K, V> entry : map.entrySet()) {
			if (entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		return null;
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof ItemFrame) {
			ItemFrame itemFrame = (ItemFrame) event.getEntity();
			ItemStack itemStack = itemFrame.getItem();
			if (itemStack == null || itemStack.getType() != Material.MAP) return;
			Collection<DisplayInfo> displays = DisplayInfo.displays.values();
			for (DisplayInfo display : displays) {
				if (!display.mapIds.contains((int) itemStack.getDurability())) continue;
				float dYaw = 90F * (Math.round(display.location.getYaw() / 90F) % 4);
				BlockFace blockFace = itemFrame.getAttachedFace();
				if (dYaw == 0) {
					if (!blockFace.equals(BlockFace.SOUTH)) continue;
				} else if (dYaw == 90) {
					if (!blockFace.equals(BlockFace.WEST)) continue;
				} else if (dYaw == 180) {
					if (!blockFace.equals(BlockFace.NORTH)) continue;
				} else if (dYaw == 270) {
					if (!blockFace.equals(BlockFace.EAST)) continue;
				}
				Block block = itemFrame.getLocation().clone().add(blockFace.getModX(), blockFace.getModY(), blockFace.getModZ()).getBlock();
				if (ClickOnScreen.numBetween(block.getX(), display.location.getX(), display.locEnd.getX()) && ClickOnScreen.numBetween(block.getY(), display.location.getY(), display.locEnd.getY()) && ClickOnScreen.numBetween(block.getZ	(), display.location.getZ(), display.locEnd.getZ())) {
					event.setCancelled(true);
					if (event.getDamager() instanceof Player) {
						Player player = (Player) event.getDamager();
						if (!player.hasPermission("ayunmcvnc.interact")) return;
						ClickOnScreen.clickedOnBlock(block, player, true);
						break;
					}
				}
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Collection<DisplayInfo> displays = DisplayInfo.displays.values();
		BlockFace blockFace = ClickOnScreen.getBlockFace(player, 5).getOppositeFace();
		for (DisplayInfo display : displays) {
			float dYaw = 90F * (Math.round(display.location.getYaw() / 90F) % 4);
			if (dYaw == 0) {
				if (!blockFace.equals(BlockFace.SOUTH)) continue;
			} else if (dYaw == 90) {
				if (!blockFace.equals(BlockFace.WEST)) continue;
			} else if (dYaw == 180) {
				if (!blockFace.equals(BlockFace.NORTH)) continue;
			} else if (dYaw == 270) {
				if (!blockFace.equals(BlockFace.EAST)) continue;
			}
			Block block = event.getBlock();
			if (ClickOnScreen.numBetween(block.getX(), display.location.getX(), display.locEnd.getX()) && ClickOnScreen.numBetween(block.getY(), display.location.getY(), display.locEnd.getY()) && ClickOnScreen.numBetween(block.getZ	(), display.location.getZ(), display.locEnd.getZ())) {
				event.setCancelled(true);
				if (!player.hasPermission("ayunmcvnc.interact")) return;
				ClickOnScreen.clickedOnBlock(block, player, true);
				break;
			}
		}
	}

	@EventHandler
	public void onHotbarChange(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		if (!player.hasPermission("ayunmcvnc.interact")) return;
		Collection<DisplayInfo> displays = DisplayInfo.displays.values();
		BlockFace blockFace = ClickOnScreen.getBlockFace(player, 5).getOppositeFace();
		for (DisplayInfo display : displays) {
			if (display.paused) continue;
			if (!display.mouse) continue;
			float dYaw = 90F * (Math.round(display.location.getYaw() / 90F) % 4);
			if (dYaw == 0) {
				if (!blockFace.equals(BlockFace.SOUTH)) continue;
			} else if (dYaw == 90) {
				if (!blockFace.equals(BlockFace.WEST)) continue;
			} else if (dYaw == 180) {
				if (!blockFace.equals(BlockFace.NORTH)) continue;
			} else if (dYaw == 270) {
				if (!blockFace.equals(BlockFace.EAST)) continue;
			}
			Block block = player.getTargetBlock(null, 5);
			if (ClickOnScreen.numBetween(block.getX(), display.location.getX(), display.locEnd.getX()) && ClickOnScreen.numBetween(block.getY(), display.location.getY(), display.locEnd.getY()) && ClickOnScreen.numBetween(block.getZ	(), display.location.getZ(), display.locEnd.getZ())) {
				ClickOnScreen.sendActionBar(player, "ayunMCVNC: Current tool: " + Main.plugin.slotTexts.get(event.getNewSlot()));
				break;
			}
		}
	}
}