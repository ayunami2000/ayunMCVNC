package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.DisplayInfo;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class CommandKeystate extends AyunCommand {
	CommandKeystate() {
		super("keystate", "ayunmcvnc.interact", new int[] {1, -1, 0});
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (args.length < 3) {
			sendUsage(sender, "<name> <down (e.g. true)> <keyname(s)>   §l(Warning: Case sensitive!)");
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
		boolean pressed = Boolean.parseBoolean(args[1]);
		String[] trimArgs = Arrays.copyOfRange(args, 2, args.length);
		for (String arg : trimArgs) {
			displayInfo.videoCapture.pressKey(arg, pressed);
		}
	}
}
