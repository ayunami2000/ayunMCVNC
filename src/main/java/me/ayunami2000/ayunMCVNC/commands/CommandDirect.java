package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.DisplayInfo;
import me.ayunami2000.ayunMCVNC.ImageManager;
import org.bukkit.command.CommandSender;

public class CommandDirect extends AyunCommand {
	CommandDirect() {
		super("direct", "ayunmcvnc.manage", new int[] {1, -1});
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (args.length < 1) {
			sendUsage(sender, "<name> [username|nothing_to_disable]");
			return;
		}
		DisplayInfo displayInfo = getDisplay(sender, args[0]);
		if (displayInfo == null) {
			sendError(sender, "Invalid display!");
			return;
		}
		if (args.length < 2) {
			displayInfo.setDirectController(null);
			sendMessage(sender, "Display direct control disabled!");
		} else {
			displayInfo.setDirectController(args[1]);
			sendMessage(sender, "Display direct controller set!");
		}
		ImageManager.getInstance().saveImage(displayInfo);
	}
}
