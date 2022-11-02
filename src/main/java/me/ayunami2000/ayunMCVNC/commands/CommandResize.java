package me.ayunami2000.ayunMCVNC.commands;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import me.ayunami2000.ayunMCVNC.DisplayInfo;
import me.ayunami2000.ayunMCVNC.ImageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.ArrayList;
import java.util.List;

public class CommandResize extends AyunCommand {
	CommandResize() {
		super("resize", "ayunmcvnc.manage", new int[] {1, -1});
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (args.length < 3) {
			sendUsage(sender, "<name> <width> <height>");
			return;
		}
		DisplayInfo displayInfo = getDisplay(sender, args[0]);
		if (displayInfo == null) {
			sendError(sender, "Invalid display!");
			return;
		}
		if (displayInfo.directController != null) {
			sendError(sender, "You cannot resize a display that is in direct control mode!");
			return;
		}
		int width = Integer.parseInt(args[1]);
		int height = Integer.parseInt(args[2]);
		DisplayInfo.Shell shell = new DisplayInfo.Shell(displayInfo, true);
		shell.width = width;

		List<Integer> mapIds = new ArrayList<>(width * height);

		for (int i = 0; i < width * height; i++) {
			int potentialMapId = ImageManager.getInstance().reuse();
			MapView mapView = potentialMapId == -1 ? Bukkit.createMap(shell.location.getWorld()) : Bukkit.getMap((short) potentialMapId);
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
			((Player) sender).getInventory().addItem(itemStack);

			mapIds.add((int) mapView.getId());
		}

		shell.mapIds = mapIds;
		displayInfo = shell.create();
		ImageManager.getInstance().saveImage(displayInfo);
		sendMessage(sender, "Display resized!");
	}
}
