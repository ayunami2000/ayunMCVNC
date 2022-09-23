package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.ClickOnScreen;
import me.ayunami2000.ayunMCVNC.DisplayInfo;
import me.ayunami2000.ayunMCVNC.ImageManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class CommandMove extends AyunCommand {
	CommandMove() {
		super("mouse", "ayunmcvnc.manage", new int[] {1, -1});
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (!senderType.isPlayer) {
			sendError(sender, "Currently, you must be a player in-game so that the screen location can be set!");
			return;
		}
		Player player = (Player) sender;
		if (args.length < 1) {
			sendUsage(sender, "<name>");
			return;
		}
		DisplayInfo displayInfo = getDisplay(sender, args[0]);
		if (displayInfo == null) {
			sendError(sender, "Invalid display!");
			return;
		}
		Location loc = player.getTargetBlock(null, 5).getLocation();
		int num = Arrays.asList(ClickOnScreen.numberToBlockFace).indexOf(ClickOnScreen.getBlockFace(player, 5).getOppositeFace());
		if (num == -1) {
			sendError(sender, "You must be looking at the top left corner of the screen!");
			return;
		}
		loc.setWorld(player.getWorld());
		loc.setYaw(num * 90);
		loc.setPitch(0);
		displayInfo.location = loc;
		displayInfo.setEndLoc();
		ImageManager.getInstance().saveImage(displayInfo);
		sendMessage(sender, "Display moved!");
	}
}
