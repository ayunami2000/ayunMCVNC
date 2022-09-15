package me.ayunami2000.ayunMCVNC;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.java_websocket.WebSocket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class AudioProcessorTask extends BukkitRunnable {
	private final Object lock = new Object();
	private AudioServer ws;

	AudioProcessorTask() {
		this.ws = new AudioServer(new InetSocketAddress(InetAddress.getLoopbackAddress(), 8819));
		this.ws.start();
	}

	public synchronized void cancel() throws IllegalStateException {
		try {
			this.ws.stop();
		} catch (InterruptedException e) {
		}
		super.cancel();
	}

	@Override
	public void run() {
		if (this.ws == null) return;
		synchronized (lock) {
			Collection<DisplayInfo> displays = DisplayInfo.displays.values();
			for (DisplayInfo display : displays) {
				if (!display.audio) continue;
				if (display.currentAudio.size() == 0) continue;

				byte[] aud = display.currentAudio.toByteArray();
				display.currentAudio.reset();

				Location myPos = display.location.clone().add(display.locEnd).multiply(0.5);

				List<Player> playerList = Bukkit.getOnlinePlayers().stream().filter(player -> player.getLocation().distanceSquared(myPos) <= 256).collect(Collectors.toList());

				if (playerList.isEmpty()) continue;

				List<String> names = playerList.stream().map(player -> player.getName()).collect(Collectors.toList());

				for (WebSocket webSocket : this.ws.wsList.keySet()) {
					String s = this.ws.wsList.get(webSocket);
					if (names.contains(s)) {
						Player player = Bukkit.getPlayerExact(s);
						Location loc = player.getLocation();
						float yaw = loc.getYaw();
						float pitch = loc.getPitch();
						Vector pos = new Vector(loc.getX() - myPos.getX(), loc.getY() - myPos.getY(), loc.getZ() - myPos.getZ());
						pos = Main.rotateVectorCC(pos, new Vector(0, 1, 0), (float) (yaw * Math.PI / 180.0));
						pos = Main.rotateVectorCC(pos, new Vector(1, 0, 0), (float) (pitch * Math.PI / 180.0));
						//pos = new Vec3d(pos.x * Math.cos(yaw) + pos.z * Math.sin(yaw), pos.y - (pitch / 90), pos.z * Math.cos(yaw) - pos.x * Math.sin(yaw));
						if (webSocket.isOpen()) {
							webSocket.send(pos.getX() + "," + pos.getY() + "," + pos.getZ());
							webSocket.send(aud);
						}
					}
				}
			}
		}
	}
}
