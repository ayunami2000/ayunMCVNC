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
import java.util.UUID;

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
			String displayId = displayInfo.uuid.toString();
			ConfigurationSection displays = getData().getConfigurationSection("displays");
			if (!displays.contains(displayId)) {
				displays.createSection(displayId);
			}
			ConfigurationSection displayProperties = displays.getConfigurationSection(displayId);
			displayProperties.set("mapIds", displayInfo.mapIds);
			displayProperties.set("dither", displayInfo.dither);
			displayProperties.set("mouse", displayInfo.mouse);
			displayProperties.set("keys", displayInfo.keys);
			displayProperties.set("audio", displayInfo.audio);
			ConfigurationSection location = displayProperties.createSection("location");
			location.set("world", displayInfo.location.getWorld().getName());
			location.set("x", displayInfo.location.getBlockX());
			location.set("y", displayInfo.location.getBlockY());
			location.set("z", displayInfo.location.getBlockZ());
			location.set("dir", (int) displayInfo.location.getYaw() / 90);
			displayProperties.set("width", displayInfo.width);
			displayProperties.set("vnc", displayInfo.vnc);
			displayProperties.set("paused", displayInfo.paused);
		}
		saveData();
	}

	private void loadImages() {
		if (getData().contains("displays")) {
			ConfigurationSection displays = getData().getConfigurationSection("displays");
			Set<String> displayIds = displays.getKeys(false);
			for (String displayId : displayIds) {
				ConfigurationSection displayProperties = displays.getConfigurationSection(displayId);
				ConfigurationSection location = displayProperties.getConfigurationSection("location");
				Location loc = new Location(Main.plugin.getServer().getWorld(location.getString("world")), location.getInt("x"), location.getInt("y"), location.getInt("z"), location.getInt("dir") * 90, 0);
				new DisplayInfo(UUID.fromString(displayId), displayProperties.getIntegerList("mapIds"), displayProperties.getBoolean("dither"), displayProperties.getBoolean("mouse"), displayProperties.getBoolean("keys"), displayProperties.getBoolean("audio"), loc, displayProperties.getInt("width"), displayProperties.getString("vnc"), displayProperties.getBoolean("paused"));
			}
		}
	}

	public boolean hasImage(int id) {
		return DisplayInfo.displays.values().stream().anyMatch(displayInfo -> displayInfo.mapIds.contains(id));
	}

	public void removeImage(UUID displayId) {
		if (getData().contains("displays")) {
			ConfigurationSection displays = getData().getConfigurationSection("displays");
			if (displays.isSet(displayId.toString())) {
				displays.set(displayId.toString(), null);
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