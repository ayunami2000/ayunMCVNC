package me.ayunami2000.ayunMCVNC;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {
	public static Main plugin;

	public static List<BukkitTask> tasks = new ArrayList<>();

	@Override
	public void onEnable() {
		plugin = this;
		saveDefaultConfig();
		ImageManager manager = ImageManager.getInstance();
		manager.init();

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this, this);
		pm.registerEvents(new ScreenClickEvent(), this);

		FramePacketSender framePacketSender = new FramePacketSender();
		tasks.add(framePacketSender.runTaskTimerAsynchronously(this, 0, 1));

		tasks.add(new BukkitRunnable() {
			@Override
			public void run() {
				Set<Player> players = new HashSet<>(Bukkit.getOnlinePlayers());
				for (Player player : players) {
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
		for (BukkitTask task : tasks) task.cancel();
		tasks.clear();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (!command.getName().equals("mcvnc")) return false;
		boolean console = sender instanceof ConsoleCommandSender;
		boolean op = sender.isOp();
		if (!op && !console) {
			sender.sendMessage("Error: You do not have permission to use this command!");
			return true;
		}
		String firstArg = args.length == 0 ? "" : args[0];
		switch (firstArg) {
			case "create":
				// width
				// height
				// dither
				// mouse
				// keys (todo: actually use)
				// audio (todo: implement)
				// ip:port
				// paused (optional)
				if (args.length < 8) {
					sender.sendMessage("Usage: /mcvnc create <width (e.g. 5)> <height (e.g. 4)> <dither (e.g. true)> <mouse> <keys> <audio> <ip:port> [paused]");
					return true;
				}
				if (!(sender instanceof Player)) {
					sender.sendMessage("Error: Currently, you must be a player in-game so that the screen location can be set!");
					return true;
				}
				Location loc = ((Player) sender).getTargetBlock(null, 5).getLocation();
				int yaw = (int) ((Player) sender).getLocation().getYaw();
				while (yaw < 0) yaw += 360;
				yaw = yaw % 360;
				loc.setYaw(yaw);
				loc.setPitch(0);

				int width = Integer.parseInt(args[1]);
				int height = Integer.parseInt(args[2]);

				boolean dither = Boolean.parseBoolean(args[3]);
				boolean mouse = Boolean.parseBoolean(args[4]);
				boolean keys = Boolean.parseBoolean(args[5]);
				boolean audio = Boolean.parseBoolean(args[6]);

				String vnc = args[7];

				boolean paused = args.length < 9 ? false : Boolean.parseBoolean(args[8]);

				List<Integer> mapIds = new ArrayList<>(width * height);

				for (int i = 0; i < width * height; i++) {
					MapView mapView = getServer().createMap(loc.getWorld());
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
				DisplayInfo displayInfo = new DisplayInfo(UUID.randomUUID(), mapIds, dither, mouse, keys, audio, loc, width, vnc, paused);
				ImageManager.getInstance().saveImage(displayInfo);
				sender.sendMessage("Display successfully created! UUID: " + displayInfo.uuid.toString());
				return true;
			case "delete":
				if (args.length < 2) {
					sender.sendMessage("Usage: /mcvnc delete <uuid>");
					return true;
				}
				DisplayInfo displayInfoo = DisplayInfo.displays.getOrDefault(UUID.fromString(args[1]), null);
				if (displayInfoo == null) {
					sender.sendMessage("Error: Invalid display!");
				} else {
					displayInfoo.paused = true;
					displayInfoo.delete();
					ImageManager.getInstance().removeImage(displayInfoo.uuid);
				}
				sender.sendMessage("Display successfully deleted!");
				return true;
			case "toggle":
				if (args.length < 2) {
					sender.sendMessage("Usage: /mcvnc toggle <uuid>");
					return true;
				}
				DisplayInfo displayInfoooo = DisplayInfo.displays.getOrDefault(UUID.fromString(args[1]), null);
				if (displayInfoooo == null) {
					sender.sendMessage("Error: Invalid display!");
				} else {
					displayInfoooo.paused = !displayInfoooo.paused;
					ImageManager.getInstance().saveImage(displayInfoooo);
				}
				sender.sendMessage("Display toggled!");
				return true;
			case "list":
				sender.sendMessage("Displays:");
				Set<UUID> displayIds = DisplayInfo.displays.keySet();
				if (displayIds.size() == 0) {
					sender.sendMessage("(there are no displays...)");
				} else {
					for (UUID displayId : displayIds) {
						sender.sendMessage(" -> " + displayId.toString());
					}
				}
				return true;
			case "cb":
				if (args.length > 2) {
					DisplayInfo displayInfooo = DisplayInfo.displays.getOrDefault(UUID.fromString(args[1]), null);
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
					sender.sendMessage("Usage: /mcvnc cb <uuid> [key|mouse]");
				}
				return true;
			case "type":
				if (args.length == 1 || args.length == 2) {
					sender.sendMessage("Usage: /mcvnc type <uuid> <text>");
				} else {
					DisplayInfo displayInfooo = DisplayInfo.displays.getOrDefault(UUID.fromString(args[1]), null);
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
				if (args.length == 1 || args.length == 2) {
					sender.sendMessage("Usage: /mcvnc key <uuid> <keyname>\n§lWarning: Case sensitive!");
				} else {
					DisplayInfo displayInfooo = DisplayInfo.displays.getOrDefault(UUID.fromString(args[1]), null);
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
			case "press":
				if (args.length == 1 || args.length == 2 || args.length == 3) {
					sender.sendMessage("Usage: /mcvnc press <uuid> <duration> <keyname>\n§lWarning: Case sensitive!");
				} else {
					DisplayInfo displayInfooo = DisplayInfo.displays.getOrDefault(UUID.fromString(args[1]), null);
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
				sender.sendMessage("usage: /mcvnc [create|delete|toggle|list|cb|type|key|press] [...]");
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
