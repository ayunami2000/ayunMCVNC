package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.DisplayInfo;
import me.ayunami2000.ayunMCVNC.ImageManager;
import org.bukkit.command.CommandSender;

public class CommandDelay extends AyunCommand {
	CommandDelay() {
		super("delay", "ayunmcvnc.manage");
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (args.length < 2) {
			sendUsage(sender, "<name> <video_delay|disable_audio_with_negative_value>");
			return;
		}
		DisplayInfo displayInfo = getDisplay(sender, args[0]);
		if (displayInfo == null) {
			sendError(sender, "Invalid display!");
			return;
		}
		int audio = Integer.parseInt(args[1]);
		DisplayInfo.Shell shell = new DisplayInfo.Shell(displayInfo, false);
		shell.audio = audio;
		displayInfo = shell.create();
		ImageManager.getInstance().saveImage(displayInfo);
		sendMessage(sender, "Display video delay set!");
	}
}
