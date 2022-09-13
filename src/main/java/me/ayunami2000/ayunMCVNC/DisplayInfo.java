package me.ayunami2000.ayunMCVNC;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DisplayInfo {
	public static final Map<UUID, DisplayInfo> displays = new HashMap<>();

	public static final Set<Integer> screenPartModified = new HashSet<>();

	public final List<Integer> mapIds;
	public final UUID uuid;
	public boolean dither;
	public boolean mouse;
	public boolean keys;
	public boolean audio;
	public Location location; // top left corner
	public int width;
	public String vnc;
	public boolean paused;

	public BufferedImage currentFrame = null;
	public VideoCapture videoCapture;
	private BukkitTask task1;

	public DisplayInfo(UUID uuid, List<Integer> mapIds, boolean dither, boolean mouse, boolean keys, boolean audio, Location location, int width, String vnc, boolean paused) {
		this.uuid = uuid;
		this.mapIds = mapIds;
		this.dither = dither;
		this.mouse = mouse;
		this.keys = keys;
		this.audio = audio;
		this.location = location;
		this.width = width;
		this.vnc = vnc;
		this.paused = paused;

		displays.put(this.uuid, this);

		this.videoCapture = new VideoCapture(this);
		this.videoCapture.start();

		FrameProcessorTask frameProcessorTask = new FrameProcessorTask(this, this.mapIds.size(), this.width);
		Main.tasks.add(task1 = frameProcessorTask.runTaskTimerAsynchronously(Main.plugin, 0, 1));
	}

	public void delete() {
		displays.remove(this.uuid);
		if (videoCapture != null) videoCapture.cleanup();
		Main.tasks.remove(task1);
		task1.cancel();
	}

	public static void delete(UUID uuid) {
		displays.get(uuid).delete();
	}
}
