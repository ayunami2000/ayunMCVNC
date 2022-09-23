package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.DisplayInfo;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class CommandPress extends AyunCommand {
	CommandPress() {
		super("press", "ayunmcvnc.interact");
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (args.length < 2) {
			sendUsage(sender, "<name> <duration> <keyname(s)>   §l(Warning: Case sensitive!)");
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
		try {
			int dur = Math.abs(Integer.parseInt(args[1]));
			String[] trimArgs = Arrays.copyOfRange(args, 2, args.length);
			for (String arg : trimArgs) {
				displayInfo.videoCapture.holdKey(arg, dur);
			}
		} catch (NumberFormatException e) {
			sendError(sender, "Duration must be a valid integer in milliseconds!");
		}
	}
}
