package me.ayunami2000.ayunMCVNC.commands;

import me.ayunami2000.ayunMCVNC.DisplayInfo;
import me.ayunami2000.ayunMCVNC.ImageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.ArrayList;
import java.util.List;

public class CommandDirect extends AyunCommand {
	CommandDirect() {
		super("direct", "ayunmcvnc.manage", new int[] {1, -1});
	}

	@Override
	public void run(CommandSender sender, String[] args, SenderType senderType) {
		if (args.length < 1) {
			sendUsage(sender, "<name> [direct_controller_username|disable_by_not_specifying]");
			return;
		}
		String directController = args.length < 2 ? null : args[1];
		DisplayInfo displayInfo = getDisplay(sender, args[0]);
		if (displayInfo == null) {
			sendError(sender, "Invalid display!");
			return;
		}
		if (directController != null && (displayInfo.width != 6 || displayInfo.mapIds.size() != 6)) {
			int width = 6;
			int height = 1;
			DisplayInfo.Shell shell = new DisplayInfo.Shell(displayInfo, true);
			shell.width = width;

			List<Integer> mapIds = new ArrayList<>(width * height);

			for (int i = 0; i < width * height; i++) {
				int potentialMapId = ImageManager.getInstance().reuse();
				MapView mapView = potentialMapId == -1 ? Bukkit.createMap(shell.location.getWorld()) : Bukkit.getMap((short) potentialMapId);
				mapView.setScale(MapView.Scale.CLOSEST);
				mapView.setUnlimitedTracking(true);
				for (MapRenderer renderer : mapView.getRenderers()) {
					mapView.removeRenderer(renderer);
				}
				ItemStack itemStack = new ItemStack(Material.MAP);
				MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
				mapMeta.setMapView(mapView);
				itemStack.setItemMeta(mapMeta);
				((Player) sender).getInventory().addItem(itemStack);

				mapIds.add((int) mapView.getId());
			}

			shell.mapIds = mapIds;
			shell.directController = directController;
			displayInfo = shell.create();
		} else {
			displayInfo.setDirectController(directController);
		}
		ImageManager.getInstance().saveImage(displayInfo);
		sendMessage(sender, "Display direct controller set!");
	}
}
