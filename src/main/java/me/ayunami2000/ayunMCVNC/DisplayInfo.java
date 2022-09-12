package me.ayunami2000.ayunMCVNC;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DisplayInfo {
	public static final Map<UUID, DisplayInfo> displays = new HashMap<>();
	public static final Set<ScreenPart> screenParts = new HashSet<>();

	public final List<Integer> mapIds;
	public final UUID uuid;
	public boolean dither;
	public boolean mouse;
	public boolean keys;
	public boolean audio;
	public Location location; // top left corner
	public int width;
	public String vnc;

	public DisplayInfo(UUID uuid, List<Integer> mapIds, boolean dither, boolean mouse, boolean keys, boolean audio, Location location, int width, String vnc) {
		this.uuid = uuid;
		this.mapIds = mapIds;
		this.dither = dither;
		this.mouse = mouse;
		this.keys = keys;
		this.audio = audio;
		this.location = location;
		this.width = width;
		this.vnc = vnc;

		displays.put(this.uuid, this);
	}

	public void delete() {
		displays.remove(this.uuid);
	}

	public static void delete(UUID uuid) {
		displays.remove(uuid);
	}
}
