package me.ayunami2000.ayunMCVNC;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
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

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this, this);
		pm.registerEvents(new ScreenClickEvent(), this);

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
		if (!command.getName().equals("mcvnc")) return false;
		boolean console = sender instanceof ConsoleCommandSender;
		boolean canManage = console || sender.hasPermission("ayunmcvnc.manage");
		boolean canInteract = canManage || sender.hasPermission("ayunmcvnc.interact");
		String firstArg = args.length == 0 ? "" : args[0];
		switch (firstArg) {
			case "create":
				if (!canManage) {
					sender.sendMessage("Error: You do not have permission to create displays!");
					return true;
				}
				// name
				// width
				// height
				// dither
				// mouse
				// mjpeg
				// audio (todo: implement)
				// ip:port
				// paused (optional)
				if (args.length < 7) {
					sender.sendMessage("Usage: /mcvnc create <name> <width (e.g. 5)> <height (e.g. 4)> <mouse> <audio> <vnc_ip:port|mjpeg_url|udp_port>[;audioport][;vnc_uname][;vnc_passwd] [paused]");
					return true;
				}
				if (!(sender instanceof Player)) {
					sender.sendMessage("Error: Currently, you must be a player in-game so that the screen location can be set!");
					return true;
				}
				String newName = args[1].toLowerCase();
				if (newName.startsWith("@")) {
					sender.sendMessage("Error: A display's name cannot start with '@'!");
					return true;
				}
				if (DisplayInfo.displays.containsKey(newName)) {
					sender.sendMessage("Error: A display with that name already exists!");
					return true;
				}
				Location loc = ((Player) sender).getTargetBlock(null, 5).getLocation();
				int yaw = (int) ((Player) sender).getLocation().getYaw();
				while (yaw < 0) yaw += 360;
				yaw = yaw % 360;
				loc.setYaw(yaw);
				loc.setPitch(0);

				int width = Integer.parseInt(args[2]);
				int height = Integer.parseInt(args[3]);

				boolean mouse = Boolean.parseBoolean(args[4]);
				boolean audio = Boolean.parseBoolean(args[5]);

				String dest = args[6];

				boolean paused = args.length < 8 ? false : Boolean.parseBoolean(args[7]);

				List<Integer> mapIds = new ArrayList<>(width * height);

				for (int i = 0; i < width * height; i++) {
					int potentialMapId = ImageManager.getInstance().reuse();
					MapView mapView = potentialMapId == -1 ? getServer().createMap(loc.getWorld()) : getServer().getMap((short) potentialMapId);
					mapView.setScale(MapView.Scale.CLOSEST);
					mapView.setUnlimitedTracking(true);
					for (MapRenderer renderer : mapView.getRenderers()) {
						mapView.removeRenderer(renderer);
					}
					ItemStack itemStack = new ItemStack(Material.MAP);
					itemStack.setDurability(mapView.getId());
					((Player) sender).getInventory().addItem(itemStack);

					mapIds.add((int) mapView.getId());
				}
				DisplayInfo displayInfo = new DisplayInfo(newName, mapIds, mouse, audio, loc, width, dest, paused);
				ImageManager.getInstance().saveImage(displayInfo);
				sender.sendMessage("Display successfully created! Name: " + displayInfo.name);
				return true;
			case "delete":
				if (!canManage) {
					sender.sendMessage("Error: You do not have permission to delete displays!");
					return true;
				}
				if (args.length < 2) {
					sender.sendMessage("Usage: /mcvnc delete <name>");
					return true;
				}
				DisplayInfo displayInfoo = args[1].startsWith("@") ? DisplayInfo.getNearest(sender) : DisplayInfo.displays.getOrDefault(args[1], null);
				if (displayInfoo == null) {
					sender.sendMessage("Error: Invalid display!");
				} else {
					displayInfoo.delete(true);
					ImageManager.getInstance().removeImage(displayInfoo.name);
					ImageManager.getInstance().saveData();
				}
				sender.sendMessage("Display successfully deleted!");
				return true;
			case "toggle":
				if (!canManage) {
					sender.sendMessage("Error: You do not have permission to toggle displays!");
					return true;
				}
				if (args.length < 2) {
					sender.sendMessage("Usage: /mcvnc toggle <name>");
					return true;
				}
				DisplayInfo displayInfoooo = args[1].startsWith("@") ? DisplayInfo.getNearest(sender) : DisplayInfo.displays.getOrDefault(args[1], null);
				if (displayInfoooo == null) {
					sender.sendMessage("Error: Invalid display!");
				} else {
					displayInfoooo.paused = !displayInfoooo.paused;
					ImageManager.getInstance().saveImage(displayInfoooo);
				}
				sender.sendMessage("Display toggled!");
				return true;
			case "move":
				if (!canManage) {
					sender.sendMessage("Error: You do not have permission to move displays!");
					return true;
				}
				if (args.length < 2) {
					sender.sendMessage("Usage: /mcvnc move <name>");
					return true;
				}
				DisplayInfo displayInfooooo = args[1].startsWith("@") ? DisplayInfo.getNearest(sender) : DisplayInfo.displays.getOrDefault(args[1], null);
				if (displayInfooooo == null) {
					sender.sendMessage("Error: Invalid display!");
				} else {
					Location locc = ((Player) sender).getTargetBlock(null, 5).getLocation();
					int yaww = (int) ((Player) sender).getLocation().getYaw();
					while (yaww < 0) yaww += 360;
					yaww = yaww % 360;
					locc.setYaw(yaww);
					locc.setPitch(0);
					displayInfooooo.location = locc;
					displayInfooooo.setEndLoc();
					ImageManager.getInstance().saveImage(displayInfooooo);
				}
				sender.sendMessage("Display moved!");
				return true;
			case "list":
				if (!canInteract) {
					sender.sendMessage("Error: You do not have permission to list displays!");
					return true;
				}
				sender.sendMessage("Displays:");
				Set<String> displayIds = DisplayInfo.displays.keySet();
				if (displayIds.size() == 0) {
					sender.sendMessage("(there are no displays...)");
				} else {
					sender.sendMessage(" -> @ (nearest display)");
					for (String displayId : displayIds) {
						sender.sendMessage(" -> " + displayId);
					}
				}
				return true;
			case "cb":
				if (!canManage) {
					sender.sendMessage("Error: You do not have permission to run this command!");
					return true;
				}
				if (args.length > 2) {
					DisplayInfo displayInfooo = args[1].startsWith("@") ? DisplayInfo.getNearest(sender) : DisplayInfo.displays.getOrDefault(args[1], null);
					if (displayInfooo == null) {
						sender.sendMessage("Error: Invalid display!");
					} else {
						if (displayInfooo.paused) {
							sender.sendMessage("Error: This display is currently paused!");
							return true;
						}
						switch (args[2]) {
							case "key":
								if (args.length > 3) {
									String[] trimargs = Arrays.copyOfRange(args, 3, args.length);
									for (String arg : trimargs) {
										displayInfooo.videoCapture.holdKey(arg);
									}
								} else {
									sender.sendMessage("Error: Invalid usage!");
								}
								break;
							case "mouse":
								if (args.length == 6) {
									sender.sendMessage("Error: Not yet implemented!");
								} else {
									sender.sendMessage("Error: Invalid usage!");
								}
								break;
							default:
								sender.sendMessage("Error: Invalid command arguments!");
						}
					}
				} else {
					sender.sendMessage("Usage: /mcvnc cb <name> [key|mouse]");
				}
				return true;
			case "type":
				if (!canInteract) {
					sender.sendMessage("Error: You do not have permission to type!");
					return true;
				}
				if (args.length == 1 || args.length == 2) {
					sender.sendMessage("Usage: /mcvnc type <name> <text>");
				} else {
					DisplayInfo displayInfooo = args[1].startsWith("@") ? DisplayInfo.getNearest(sender) : DisplayInfo.displays.getOrDefault(args[1], null);
					if (displayInfooo == null) {
						sender.sendMessage("Error: Invalid display!");
					} else {
						if (displayInfooo.paused) {
							sender.sendMessage("Error: This display is currently paused!");
							return true;
						}
						displayInfooo.videoCapture.typeText(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
					}
				}
				return true;
			case "key":
				if (!canInteract) {
					sender.sendMessage("Error: You do not have permission to press keys!");
					return true;
				}
				if (args.length == 1 || args.length == 2) {
					sender.sendMessage("Usage: /mcvnc key <name> <keyname>\n§lWarning: Case sensitive!");
				} else {
					DisplayInfo displayInfooo = args[1].startsWith("@") ? DisplayInfo.getNearest(sender) : DisplayInfo.displays.getOrDefault(args[1], null);
					if (displayInfooo == null) {
						sender.sendMessage("Error: Invalid display!");
					} else {
						if (displayInfooo.paused) {
							sender.sendMessage("Error: This display is currently paused!");
							return true;
						}
						String[] trimargs = Arrays.copyOfRange(args, 2, args.length);
						for (String arg : trimargs) {
							displayInfooo.videoCapture.pressKey(arg);
						}
					}
				}
				return true;
			case "keystate":
				if (!canInteract) {
					sender.sendMessage("Error: You do not have permission to change key states!");
					return true;
				}
				if (args.length == 1 || args.length == 2 || args.length == 3) {
					sender.sendMessage("Usage: /mcvnc keystate <name> <down|up> <keyname>\n§lWarning: Case sensitive!");
				} else {
					DisplayInfo displayInfooo = args[1].startsWith("@") ? DisplayInfo.getNearest(sender) : DisplayInfo.displays.getOrDefault(args[1], null);
					if (displayInfooo == null) {
						sender.sendMessage("Error: Invalid display!");
					} else {
						if (displayInfooo.paused) {
							sender.sendMessage("Error: This display is currently paused!");
							return true;
						}
						boolean pressed = args[2].equalsIgnoreCase("down");
						String[] trimargs = Arrays.copyOfRange(args, 3, args.length);
						for (String arg : trimargs) {
							displayInfooo.videoCapture.pressKey(arg, pressed);
						}
					}
				}
				return true;
			case "press":
				if (!canInteract) {
					sender.sendMessage("Error: You do not have permission to hold keys!");
					return true;
				}
				if (args.length == 1 || args.length == 2 || args.length == 3) {
					sender.sendMessage("Usage: /mcvnc press <name> <duration> <keyname>\n§lWarning: Case sensitive!");
				} else {
					DisplayInfo displayInfooo = args[1].startsWith("@") ? DisplayInfo.getNearest(sender) : DisplayInfo.displays.getOrDefault(args[1], null);
					if (displayInfooo == null) {
						sender.sendMessage("Error: Invalid display!");
					} else {
						if (displayInfooo.paused) {
							sender.sendMessage("Error: This display is currently paused!");
							return true;
						}
						try {
							int dur = Math.abs(Integer.parseInt(args[2]));
							String[] trimargs = Arrays.copyOfRange(args, 3, args.length);
							for (String arg : trimargs) {
								displayInfooo.videoCapture.holdKey(arg, dur);
							}
						} catch (NumberFormatException e) {
							sender.sendMessage("Error: Duration must be a valid integer in milliseconds!");
						}
					}
				}
				return true;
			default:
				sender.sendMessage("usage: /mcvnc [create|delete|toggle|move|list|cb|type|key|press|keystate] [...]");
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
