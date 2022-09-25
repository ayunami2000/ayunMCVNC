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
		if (args.length < 2) {
			sendUsage(sender, "<name> [direct_controller_username|disable_by_not_specifying]");
			return;
		}
		DisplayInfo displayInfo = getDisplay(sender, args[0]);
		if (displayInfo == null) {
			sendError(sender, "Invalid display!");
			return;
		}
		displayInfo.setDirectController(args[1]);
		ImageManager.getInstance().saveImage(displayInfo);
		sendMessage(sender, "Display direct controller set!");
	}
}
