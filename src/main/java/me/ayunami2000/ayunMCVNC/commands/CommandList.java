package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.DisplayInfo;
import org.bukkit.command.CommandSender;

import java.util.Set;

public class CommandList extends AyunCommand {
	CommandList() {
		super("list", "ayunmcvnc.interact");
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		sendMessage(sender, "Displays:");
		Set<String> displayIds = DisplayInfo.displays.keySet();
		if (displayIds.size() == 0) {
			sendMessage(sender, "(there are no displays...)");
		} else {
			DisplayInfo nearest = DisplayInfo.getNearest(sender);
			String nearestName = nearest == null ? "" : (" (" + nearest.name + ")");
			sendMessage(sender, " -> @ (nearest display)" + nearestName);
			for (String displayId : displayIds) {
				sendMessage(sender, " -> " + displayId);
			}
		}
	}
}
