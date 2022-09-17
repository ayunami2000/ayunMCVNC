package me.ayunami2000.ayunMCVNC;

import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DisplayInfo {
	public static final Map<String, DisplayInfo> displays = new HashMap<>();

	public static final Set<Integer> screenPartModified = new HashSet<>();

	public static final List<Integer> unusedMapIds = new ArrayList<>();

	public final List<Integer> mapIds;
	public final String name;
	public boolean dither;
	public boolean mouse;
	public boolean vnc;
	public boolean audio;
	public Location location; // top left corner
	public Location locEnd; // bottom right corner
	public int width;
	public String dest;
	public boolean paused;

	public BufferedImage currentFrame = null;
	public OutputStream audioOs;
	public InputStream audioIs;
	public Process audioProcess;
	public VideoCapture videoCapture;
	private final BukkitTask task1;
	public int uniquePort;

	public DisplayInfo(String name, List<Integer> mapIds, boolean dither, boolean mouse, boolean vnc, boolean audio, Location location, int width, String dest, boolean paused) {
		this.name = name;
		this.mapIds = mapIds;
		this.dither = dither;
		this.mouse = mouse;
		this.vnc = vnc;
		this.audio = audio;
		this.location = location;
		this.width = width;
		this.dest = dest;
		this.paused = paused;

		this.setEndLoc();

		displays.put(this.name, this);

		this.videoCapture = new VideoCapture(this);
		this.videoCapture.start();

		FrameProcessorTask frameProcessorTask = new FrameProcessorTask(this, this.mapIds.size(), this.width);
		Main.tasks.add(task1 = frameProcessorTask.runTaskTimerAsynchronously(Main.plugin, 0, 1));

		try {
			audioProcess = new ProcessBuilder("ffmpeg", "-fflags", "nobuffer", "-f", "s16le", "-acodec", "pcm_s16le", "-ac", "2", "-ar", "44100", "-i", "pipe:", "-f", "mp3", "-codec:a", "libmp3lame", "-b:a", "128k", "-").start();
			audioIs = audioProcess.getInputStream();
			audioOs = audioProcess.getOutputStream();
			/*
			new Thread(() -> {
				try {
					InputStream err = audioProcess.getErrorStream();
					while (audioProcess.isAlive()) {
						int avail = err.available();
						byte[] errBytes = new byte[avail];
						err.read(errBytes, 0, avail);
						System.out.print(new String(errBytes));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
			*/
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setEndLoc() {
		float yaw = this.location.getYaw();

		Vector tmpDir = new Vector(0, 0, 0);
		if (yaw < 45 || yaw >= 315) {
			//south
			tmpDir = new Vector(0, 0, 1);
		} else if (yaw < 135) {
			//west
			tmpDir = new Vector(-1, 0, 0);
		} else if (yaw < 225) {
			//north
			tmpDir = new Vector(0, 0, -1);
		} else if (yaw < 315) {
			//east
			tmpDir = new Vector(1, 0, 0);
		}

		this.locEnd = this.location.clone().add(Main.rotateVectorCC(tmpDir, new Vector(0, 1, 0), -Math.PI / 2.0).multiply(this.width - 1).add(new Vector(0, 1 - this.mapIds.size() / this.width, 0)));
	}

	public void delete(boolean fr) {
		displays.remove(this.name);
		if (videoCapture != null) videoCapture.cleanup();
		Main.tasks.remove(task1);
		task1.cancel();
		if (fr) {
			unusedMapIds.addAll(this.mapIds);
		}
		if (audioProcess != null) {
			audioProcess.destroy();
		}
	}

	public void delete() {
		this.delete(false);
	}

	public static DisplayInfo getNearest(CommandSender sender) {
		return getNearest(sender, -1);
	}

	public static DisplayInfo getNearest(CommandSender sender, int hardLimit) {
		Collection<DisplayInfo> displayValues = displays.values();
		double minDist = Double.MAX_VALUE;
		DisplayInfo res = null;
		BlockCommandSender cmdBlockSender = null;
		Player player = null;
		if (sender instanceof BlockCommandSender) {
			cmdBlockSender = (BlockCommandSender) sender;
		} else if (sender instanceof Player) {
			player = (Player) sender;
		} else {
			return null;
		}
		for (DisplayInfo display : displayValues) {
			double dist = (player != null ? player.getLocation() : cmdBlockSender.getBlock().getLocation()).distanceSquared(display.location.clone().add(display.locEnd).multiply(0.5));
			if (hardLimit != -1 && dist > hardLimit) continue;
			if (minDist > dist) {
				minDist = dist;
				res = display;
			}
		}
		return res;
	}
}
