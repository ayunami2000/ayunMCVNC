package me.ayunami2000.ayunMCVNC;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;

import java.util.Set;

/***
 *
 * @author CodedRed
 * https://www.youtube.com/watch?v=DMNdcJyeP4k
 * Code Provided by CodedRed & heavily modified by ayunami2000
 */
public class ImageManager implements Listener {
	private static ImageManager instance = null;

	public static ImageManager getInstance() {
		if (instance == null)
			instance = new ImageManager();
		return instance;
	}

	public void init() {
		Bukkit.getServer().getPluginManager().registerEvents(this, Main.plugin);
		loadImages();
	}

	@EventHandler
	public void onMapInitEvent(MapInitializeEvent event) {
		if (hasImage(event.getMap().getId())) {
			MapView view = event.getMap();
			for (MapRenderer renderer : view.getRenderers())
				view.removeRenderer(renderer);
			view.setScale(Scale.CLOSEST);
			view.setUnlimitedTracking(false);
		}
	}

	public void saveImage(DisplayInfo displayInfo) {
		if (getData().contains("displays")) {
			String displayId = displayInfo.name.toString();
			ConfigurationSection displays = getData().getConfigurationSection("displays");
			if (!displays.contains(displayId)) {
				displays.createSection(displayId);
			}
			ConfigurationSection displayProperties = displays.getConfigurationSection(displayId);
			displayProperties.set("mapIds", displayInfo.mapIds);
			displayProperties.set("dither", displayInfo.dither);
			displayProperties.set("mouse", displayInfo.mouse);
			displayProperties.set("mjpeg", displayInfo.mjpeg);
			displayProperties.set("audio", displayInfo.audio);
			ConfigurationSection location = displayProperties.createSection("location");
			location.set("world", displayInfo.location.getWorld().getName());
			location.set("x", displayInfo.location.getBlockX());
			location.set("y", displayInfo.location.getBlockY());
			location.set("z", displayInfo.location.getBlockZ());
			float yaw = displayInfo.location.getYaw();
			while (yaw < 0) {
				yaw += 360;
			}
			yaw = yaw % 360;
			location.set("dir", Math.round(yaw / 90F) % 4);
			displayProperties.set("width", displayInfo.width);
			displayProperties.set("dest", displayInfo.dest);
			displayProperties.set("paused", displayInfo.paused);
		}
		saveData();
	}

	public void recycle(int mapId) {
		DisplayInfo.unusedMapIds.add(mapId);
		getData().set("unused", DisplayInfo.unusedMapIds);
		saveData();
	}

	public int reuse() {
		if (DisplayInfo.unusedMapIds.size() > 0) {
			int mapId = DisplayInfo.unusedMapIds.remove(0);
			getData().set("unused", DisplayInfo.unusedMapIds);
			// saveData(); // excessive saves; only usage ends up saving with saveImage()
			return mapId;
		} else {
			return -1;
		}
	}

	private void loadImages() {
		if (getData().contains("displays")) {
			ConfigurationSection displays = getData().getConfigurationSection("displays");
			Set<String> displayIds = displays.getKeys(false);
			for (String displayId : displayIds) {
				ConfigurationSection displayProperties = displays.getConfigurationSection(displayId);
				ConfigurationSection location = displayProperties.getConfigurationSection("location");
				Location loc = new Location(Main.plugin.getServer().getWorld(location.getString("world")), location.getInt("x"), location.getInt("y"), location.getInt("z"), location.getInt("dir") * 90F, 0);
				new DisplayInfo(displayId, displayProperties.getIntegerList("mapIds"), displayProperties.getBoolean("dither"), displayProperties.getBoolean("mouse"), displayProperties.getBoolean("mjpeg"), displayProperties.getBoolean("audio"), loc, displayProperties.getInt("width"), displayProperties.getString("dest"), displayProperties.getBoolean("paused"));
			}
		}
		if (getData().contains("unused")) {
			DisplayInfo.unusedMapIds.clear();
			DisplayInfo.unusedMapIds.addAll(getData().getIntegerList("unused"));
		}
	}

	public boolean hasImage(int id) {
		return DisplayInfo.displays.values().stream().anyMatch(displayInfo -> displayInfo.mapIds.contains(id));
	}

	public void removeImage(String displayId) {
		if (getData().contains("displays")) {
			ConfigurationSection displays = getData().getConfigurationSection("displays");
			if (displays.isSet(displayId)) {
				displays.set(displayId, null);
			}
		}
	}

	public FileConfiguration getData() {
		return Main.plugin.getConfig();
	}

	public void saveData() {
		Main.plugin.saveConfig();
	}
}