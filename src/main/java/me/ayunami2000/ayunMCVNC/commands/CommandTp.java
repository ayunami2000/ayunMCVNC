package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.ClickOnScreen;
import me.ayunami2000.ayunMCVNC.DisplayInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class CommandTp extends AyunCommand {
	CommandTp() {
		super("tp", "ayunmcvnc.manage", new int[] {1, -1});
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (senderType.isConsole) {
			sendError(sender, "This command cannot be run from console!");
			return;
		}
		if (args.length < 1) {
			sendUsage(sender, "<name>");
			return;
		}
		DisplayInfo displayInfo = getDisplay(sender, args[0]);
		if (displayInfo == null) {
			sendError(sender, "Invalid display!");
			return;
		}
		BlockFace blockFace = ClickOnScreen.numberToBlockFace[(int) (((displayInfo.location.getYaw() / 90) + 2) % 4)];
		Location targetLoc = displayInfo.location.clone().add(displayInfo.locEnd).multiply(0.5).add(0.5 + blockFace.getModX(), blockFace.getModY() - 0.5, 0.5 + blockFace.getModZ());
		if (senderType.isPlayer) {
			((Player) sender).teleport(targetLoc);
			sendMessage(sender, "Teleported to display!");
		} else if (senderType.isBlock) {
			Location blockLoc = ((BlockCommandSender) sender).getBlock().getLocation();
			double minDist = Double.MAX_VALUE;
			Player nearestPlayer = null;
			Collection<? extends Player> players = Bukkit.getOnlinePlayers();
			for (Player player : players) {
				Location playerLoc = player.getLocation();
				if (blockLoc.getWorld() != playerLoc.getWorld()) continue;
				double dist = playerLoc.distanceSquared(blockLoc);
				if (dist < minDist) {
					minDist = dist;
					nearestPlayer = player;
				}
			}
			if (nearestPlayer == null) {
				sendError(sender, "There are no nearby players!");
				return;
			}
			nearestPlayer.teleport(targetLoc);
			sendMessage(sender, "Teleported nearest player (" + nearestPlayer.getName() + ") to display!");
		}
	}
}
