package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.DisplayInfo;
import me.ayunami2000.ayunMCVNC.ImageManager;
import org.bukkit.command.CommandSender;

public class CommandAltDisplay extends AyunCommand {
	CommandAltDisplay() {
		super("altdisplay", "ayunmcvnc.manage", new int[] {1, -1});
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
		displayInfo.altDisplay = !displayInfo.altDisplay;
		ImageManager.getInstance().saveImage(displayInfo);
		sendMessage(sender, "Display mode toggled!");
	}
}
