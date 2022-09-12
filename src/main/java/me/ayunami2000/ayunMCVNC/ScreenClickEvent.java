package me.ayunami2000.ayunMCVNC;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;

public class ScreenClickEvent implements Listener {
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEntityEvent event) {
		Player player = event.getPlayer();
		Block block = player.getTargetBlock(null, 5);
		ClickOnScreen.clickedOnBlock(block, player, true);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onRightClick(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Action action = event.getAction();

		if ((action.equals(Action.RIGHT_CLICK_BLOCK)) && event.getHand() == EquipmentSlot.HAND) {
			Block block = event.getClickedBlock();
			Block tblock = player.getTargetBlock(null, 5);
			Location bloc = block.getLocation();
			Location tloc = tblock.getLocation();
			if (bloc.getX() == tloc.getX() && bloc.getY() == tloc.getY() && bloc.getZ() == tloc.getZ()) {
				ClickOnScreen.clickedOnBlock(block, player, true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Block tgtbl = player.getTargetBlock(null, 5);
		if (tgtbl != null) ClickOnScreen.clickedOnBlock(tgtbl, player, false);
	}
}