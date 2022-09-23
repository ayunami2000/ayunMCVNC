package me.ayunami2000.ayunMCVNC;

import me.ayunami2000.ayunMCVNC.commands.AyunCommand;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main extends JavaPlugin implements Listener {
	public static Main plugin;

	public static List<BukkitTask> tasks = new ArrayList<>();

	private AudioProcessorTask audioProcessorTask;

	public boolean audioUdpEnabled = false;
	public int audioUdpPortMode = 0;

	public boolean httpEnabled = true;
	public int httpPort = 8819;

	public boolean ffmpegEnabled = true;
	public List<String> ffmpegParams = new ArrayList<>();

	public int audioSampleFormat = 3;
	public int audioChannelNum = 2;
	public int audioFrequency = 48000;

	public List<String> audioSampleFormats = new ArrayList<>();
	{
		audioSampleFormats.add("u8");
		audioSampleFormats.add("s8");
		audioSampleFormats.add("u16le");
		audioSampleFormats.add("s16le");
		audioSampleFormats.add("u32le");
		audioSampleFormats.add("s32le");
	}

	public List<String> slotTexts = new ArrayList<>();
	{
		slotTexts.add("Left click");
		slotTexts.add("Middle click");
		slotTexts.add("Right click");
		slotTexts.add("Toggle left click");
		slotTexts.add("Toggle middle click");
		slotTexts.add("Toggle right click");
		slotTexts.add("No action");
		slotTexts.add("No action");
		slotTexts.add("No action");
	}

	@Override
	public void onEnable() {
		plugin = this;
		saveDefaultConfig();

		loadRestOfConfig();

		if (httpEnabled) {
			File webFolder = new File(this.getDataFolder(), "web");
			if (!webFolder.exists() || !webFolder.isDirectory()) {
				if (webFolder.exists()) {
					webFolder.delete();
				}
				webFolder.mkdir();
				try {
					InputStream link = getClass().getResourceAsStream("/index.html");
					Files.copy(link, new File(webFolder.getAbsoluteFile(), "index.html").toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		ImageManager manager = ImageManager.getInstance();
		manager.init();

		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(this, this);
		pm.registerEvents(new ScreenClickEvent(), this);

		Bukkit.getPluginCommand(AyunCommand.baseName).setTabCompleter(new TabCompletion());

		FramePacketSender framePacketSender = new FramePacketSender();
		tasks.add(framePacketSender.runTaskTimerAsynchronously(this, 0, 1));

		if (httpEnabled || audioUdpEnabled) {
			audioProcessorTask = new AudioProcessorTask();
			tasks.add(audioProcessorTask.runTaskTimerAsynchronously(this, 0, 5)); // 4 times per second
		}

		tasks.add(new BukkitRunnable() {
			@Override
			public void run() {
				Set<Player> players = new HashSet<>(Bukkit.getOnlinePlayers());
				for (Player player : players) {
					if (!player.hasPermission("ayunmcvnc.interact")) continue;
					Block tgtbl = player.getTargetBlock(null, 5);
					if (tgtbl != null) ClickOnScreen.clickedOnBlock(tgtbl, player, false);
				}
			}
		}.runTaskTimerAsynchronously(this, 0, 40)); // every 2 seconds

		AyunCommand.registerCommands();
	}

	@Override
	public void onDisable() {
		this.saveConfig();
		Set<DisplayInfo> displays = new HashSet<>(DisplayInfo.displays.values());
		for (DisplayInfo display : displays) {
			display.delete();
		}
		if (audioProcessorTask != null) {
			audioProcessorTask.cleanup();
		}
		for (BukkitTask task : tasks) task.cancel();
		tasks.clear();
	}

	private void loadRestOfConfig() {
		if (this.getConfig().contains("http")) {
			ConfigurationSection http = this.getConfig().getConfigurationSection("http");
			httpEnabled = http.getBoolean("enabled", true);
			httpPort = http.getInt("port", 8819);
		}
		if (this.getConfig().contains("audio_udp")) {
			ConfigurationSection audioUdp = this.getConfig().getConfigurationSection("audio_udp");
			audioUdpEnabled = audioUdp.getBoolean("enabled", false);
			audioUdpPortMode = Math.abs(audioUdp.getInt("method", 0)) % 2;
		}
		if (this.getConfig().contains("audio")) {
			ConfigurationSection audio = this.getConfig().getConfigurationSection("audio");
			if (audio.contains("ffmpeg")) {
				ConfigurationSection ffmpeg = audio.getConfigurationSection("ffmpeg");
				ffmpegEnabled = ffmpeg.getBoolean("enabled", true);
				ffmpegParams = ffmpeg.getStringList("params");
			}
			if (audio.contains("format")) {
				ConfigurationSection format = audio.getConfigurationSection("format");
				audioSampleFormat = Math.abs(format.getInt("sample_format", 3)) % 6;
				audioChannelNum = format.getInt("channels", 2);
				audioFrequency = format.getInt("frequency", 48000);
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (!command.getName().equalsIgnoreCase(AyunCommand.baseName)) return false;
		if (args.length > 0) {
			String subCommand = args[0].toLowerCase();
			if (AyunCommand.commandRegistry.containsKey(subCommand)) {
				AyunCommand.commandRegistry.get(subCommand).onCommand(sender, Arrays.copyOfRange(args, 1, args.length));
			} else {
				AyunCommand.sendError(sender, "Unknown sub-command!");
			}
		} else {
			AyunCommand.sendMessage(sender, "Usage: /mcvnc [create|delete|toggle|move|resize|mouse|delay|dest|list|cb|type|key|press|keystate] [...]");
		}
		return true;
	}

	public static Vector rotateVectorCC(Vector vec, Vector axis, double theta){
		double x, y, z;
		double u, v, w;
		x=vec.getX();y=vec.getY();z=vec.getZ();
		u=axis.getX();v=axis.getY();w=axis.getZ();
		double xPrime = u*(u*x + v*y + w*z)*(1d - Math.cos(theta))
				+ x*Math.cos(theta)
				+ (-w*y + v*z)*Math.sin(theta);
		double yPrime = v*(u*x + v*y + w*z)*(1d - Math.cos(theta))
				+ y*Math.cos(theta)
				+ (w*x - u*z)*Math.sin(theta);
		double zPrime = w*(u*x + v*y + w*z)*(1d - Math.cos(theta))
				+ z*Math.cos(theta)
				+ (-v*x + u*y)*Math.sin(theta);
		return new Vector(xPrime, yPrime, zPrime);
	}
}
