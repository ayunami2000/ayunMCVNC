package me.ayunami2000.ayunMCVNC.commands;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import me.ayunami2000.ayunMCVNC.ClickOnScreen;
import me.ayunami2000.ayunMCVNC.DisplayInfo;
import me.ayunami2000.ayunMCVNC.ImageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandCreate extends AyunCommand {
	CommandCreate() {
		super("create", "ayunmcvnc.manage");
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (!senderType.isPlayer) {
			sendError(sender, "Currently, you must be a player in-game so that the screen location can be set!");
			return;
		}
		Player player = (Player) sender;
		if (args.length < 6) {
			sendUsage(sender, "<name> <width (e.g. 5)> <height (e.g. 4)> <mouse (e.g. true)> <video_delay|disable_audio_with_negative_value> <vnc_ip:port|mjpeg_url|udp_port>[;audioport][;vnc_uname][;vnc_passwd] [paused] [alt_display] [direct_controller_username]");
			return;
		}
		String newName = args[0].toLowerCase();
		if (newName.startsWith("@")) {
			sendError(sender, "A display's name cannot start with '@'!");
			return;
		}
		if (DisplayInfo.displays.containsKey(newName)) {
			sendError(sender, "A display with that name already exists!");
			return;
		}
		Location loc = player.getTargetBlock(null, 5).getLocation();
		int num = Arrays.asList(ClickOnScreen.numberToBlockFace).indexOf(ClickOnScreen.getBlockFace(player, 5).getOppositeFace());
		if (num == -1) {
			sendError(sender, "You must be looking at the top left corner of the screen!");
			return;
		}
		loc.setWorld(player.getWorld());
		loc.setYaw(num * 90);
		loc.setPitch(0);

		int width = Integer.parseInt(args[1]);
		int height = Integer.parseInt(args[2]);

		boolean mouse = Boolean.parseBoolean(args[3]);
		int audio = Integer.parseInt(args[4]);

		String dest = args[5];

		boolean paused = args.length < 7 ? false : Boolean.parseBoolean(args[6]);

		boolean altDisplay = args.length < 8 ? false : Boolean.parseBoolean(args[7]);

		String directController = args.length < 9 ? null : args[8];

		if (directController != null) {
			width = 6;
			height = 1;
		}

		List<Integer> mapIds = new ArrayList<>(width * height);

		for (int i = 0; i < width * height; i++) {
			int potentialMapId = ImageManager.getInstance().reuse();
			MapView mapView = potentialMapId == -1 ? Bukkit.createMap(loc.getWorld()) : Bukkit.getMap((short) potentialMapId);
			mapView.setScale(MapView.Scale.CLOSEST);
			ImageManager.setUnlimitedTrackingSafe(mapView, true);
			for (MapRenderer renderer : mapView.getRenderers()) {
				mapView.removeRenderer(renderer);
			}
			ItemStack itemStack = MinecraftReflection.getBukkitItemStack(new ItemStack(Material.MAP));
			itemStack.setDurability(mapView.getId());
			NbtCompound mapNbt = NbtFactory.asCompound(NbtFactory.fromItemTag(itemStack));
			mapNbt.put("map", (int) mapView.getId());
			NbtFactory.setItemTag(itemStack, mapNbt);
			player.getInventory().addItem(itemStack);

			mapIds.add((int) mapView.getId());
		}
		DisplayInfo displayInfo = new DisplayInfo(newName, mapIds, mouse, audio, loc, width, dest, paused, directController, altDisplay);
		ImageManager.getInstance().saveImage(displayInfo);
		sendMessage(sender, "Display successfully created!");
	}
}
