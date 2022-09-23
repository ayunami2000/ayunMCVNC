package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.DisplayInfo;
import me.ayunami2000.ayunMCVNC.ImageManager;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class CommandType extends AyunCommand {
	CommandType() {
		super("type", "ayunmcvnc.interact", new int[] {1, -1});
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (args.length < 2) {
			sendUsage(sender, "<name> <text>");
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
		displayInfo.videoCapture.typeText(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
	}
}
