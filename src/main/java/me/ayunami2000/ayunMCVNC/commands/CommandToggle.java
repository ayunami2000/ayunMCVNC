package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.DisplayInfo;
import me.ayunami2000.ayunMCVNC.ImageManager;
import org.bukkit.command.CommandSender;

public class CommandToggle extends AyunCommand {
	CommandToggle() {
		super("toggle", "ayunmcvnc.manage", new int[] {1, -1});
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (args.length < 1) {
			sendUsage(sender, "<name>");
			return;
		}
		DisplayInfo displayInfo = getDisplay(sender, args[0]);
		if (displayInfo == null) {
			sendError(sender, "Invalid display!");
			return;
		}
		displayInfo.paused = !displayInfo.paused;
		ImageManager.getInstance().saveImage(displayInfo);
		sendMessage(sender, "Display toggled!");
	}
}
