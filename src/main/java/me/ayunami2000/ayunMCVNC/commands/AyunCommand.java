package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.DisplayInfo;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import java.util.HashMap;
import java.util.Map;

public abstract class AyunCommand {
	public static Map<String, AyunCommand> commandRegistry = new HashMap<>();

	public static void registerCommands() {
		commandRegistry.clear();
		new CommandAltDisplay();
		new CommandCb();
		new CommandCreate();
		new CommandDelay();
		new CommandDelete();
		new CommandDest();
		new CommandDirect();
		new CommandGenAltDisplay();
		new CommandKey();
		new CommandKeystate();
		new CommandList();
		new CommandMouse();
		new CommandMove();
		new CommandPress();
		new CommandResize();
		new CommandToggle();
		new CommandTp();
		new CommandType();
	}


	public static String baseName = "mcvnc";


	public final String name;
	public final Permission requiredPermission;
	public final int[] tabCompleteType;

	AyunCommand(String name, String permission) {
		this(new String[] {name}, permission, new int[] {-1});
	}

	AyunCommand(String[] names, String permission) {
		this(names, permission, new int[] {-1});
	}

	AyunCommand(String name, String permission, int[] tabCompleteType) {
		this(new String[] {name}, permission, tabCompleteType);
	}

	AyunCommand(String[] names, String permission, int[] tabCompleteType) {
		this.name = names.length == 0 ? "" : names[0];
		this.requiredPermission = new Permission(permission);
		this.tabCompleteType = tabCompleteType;
		for (String name : names) {
			commandRegistry.put(name, this);
		}
	}

	public void onCommand(CommandSender sender, String[] args) {
		if (sender.hasPermission(this.requiredPermission)) {
			run(sender, args, new SenderType(sender));
		} else {
			sendError(sender, "You do not have permission to use this command!");
		}
	}

	public int tabComplete(CommandSender sender, String[] args) {
		if (sender.hasPermission(this.requiredPermission)) {
			return this.tabCompleteType[Math.min(this.tabCompleteType.length - 1, Math.max(1, args.length) - 1)];
		} else {
			return -1;
		}
	}

	protected static class SenderType {
		public final boolean isConsole;
		public final boolean isPlayer;
		public final boolean isBlock;

		SenderType(CommandSender sender) {
			this.isConsole = sender instanceof ConsoleCommandSender;
			this.isPlayer = sender instanceof Player;
			this.isBlock = sender instanceof BlockCommandSender;
		}
	}

	protected abstract void run(CommandSender sender, String[] args, SenderType senderType);

	protected void sendUsage(CommandSender sender, String usage) {
		sender.sendMessage("Usage: /" + baseName + " " + this.name + " " + usage);
	}

	public static void sendError(CommandSender sender, String error) {
		sender.sendMessage("Error: " + error);
	}

	public static void sendMessage(CommandSender sender, String msg) {
		sender.sendMessage(msg);
	}

	public static DisplayInfo getDisplay(CommandSender sender, String name) {
		return name.startsWith("@") ? DisplayInfo.getNearest(sender) : DisplayInfo.displays.getOrDefault(name, null);
	}
}