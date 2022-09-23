package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.DisplayInfo;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class CommandKey extends AyunCommand {
	CommandKey() {
		super("key", "ayunmcvnc.interact", new int[] {1, 0});
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (args.length < 2) {
			sendUsage(sender, "<name> <keyname(s)>   §l(Warning: Case sensitive!)");
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
		String[] trimArgs = Arrays.copyOfRange(args, 1, args.length);
		for (String arg : trimArgs) {
			displayInfo.videoCapture.pressKey(arg);
		}
	}
}
