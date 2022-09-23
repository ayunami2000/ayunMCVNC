package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.DisplayInfo;
import me.ayunami2000.ayunMCVNC.ImageManager;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class CommandCb extends AyunCommand {
	CommandCb() {
		super("cb", "ayunmcvnc.manage", new int[] {1, -1});
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (args.length < 2) {
			sendUsage(sender, "<name> <key|mouse> [...]");
			return;
		}
		DisplayInfo displayInfo = getDisplay(sender, args[0]);
		if (displayInfo == null) {
			sendError(sender, "Invalid display!");
			return;
		}
		if (displayInfo.paused) {
			sendError(sender, "This display is currently paused!");
			return;
		}
		switch (args[1].toLowerCase()) {
			case "key":
				if (args.length > 2) {
					String[] trimargs = Arrays.copyOfRange(args, 3, args.length);
					for (String arg : trimargs) {
						displayInfo.videoCapture.holdKey(arg);
					}
				} else {
					sendUsage(sender, "<name> key <keys>");
				}
				break;
			case "mouse":
				// name mouse x y action
				if (args.length == 5) {
					sendError(sender, "Not yet implemented!");
				} else {
					sendUsage(sender, "<name> mouse <x> <y> <action>");
				}
				break;
			default:
				sendError(sender, "Invalid command arguments!");
		}
	}
}
