package me.ayunami2000.ayunMCVNC;

import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
	public static Main plugin;

	@Override
	public void onEnable() {
		plugin = this;
		saveDefaultConfig();
		ImageManager manager = ImageManager.getInstance();
		manager.init();

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this, this);
		pm.registerEvents(new ScreenClickEvent(), this);
	}

	@Override
	public void onDisable() {
		this.saveConfig();
	}
}
