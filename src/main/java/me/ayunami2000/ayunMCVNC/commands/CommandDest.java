package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.DisplayInfo;
import me.ayunami2000.ayunMCVNC.ImageManager;
import org.bukkit.command.CommandSender;

public class CommandDest extends AyunCommand {
	CommandDest() {
		super("dest", "ayunmcvnc.manage", new int[] {1, -1});
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (args.length < 2) {
			sendUsage(sender, "<name> <vnc_ip:port|mjpeg_url|udp_port>[;audioport][;vnc_uname][;vnc_passwd]");
			return;
		}
		DisplayInfo displayInfo = getDisplay(sender, args[0]);
		if (displayInfo == null) {
			sendError(sender, "Invalid display!");
			return;
		}
		DisplayInfo.Shell shell = new DisplayInfo.Shell(displayInfo, false);
		shell.dest = args[1];
		displayInfo = shell.create();
		ImageManager.getInstance().saveImage(displayInfo);
		sendMessage(sender, "Display destination set!");
	}
}
