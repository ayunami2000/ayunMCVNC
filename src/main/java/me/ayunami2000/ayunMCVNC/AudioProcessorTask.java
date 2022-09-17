package me.ayunami2000.ayunMCVNC;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class AudioProcessorTask extends BukkitRunnable {
	private final Object lock = new Object();
	private final AudioServer ws;

	AudioProcessorTask() {
		this.ws = new AudioServer(28819);
	}

	public synchronized void cancel() throws IllegalStateException {
		this.ws.stop();
		super.cancel();
	}

	@Override
	public void run() {
		synchronized (lock) {
			Collection<DisplayInfo> displays = DisplayInfo.displays.values();
			for (DisplayInfo display : displays) {
				if (!display.audio) continue;
				byte[] aud;
				try {
					int len = display.audioIs.available();
					if (len == 0) continue;
					aud = new byte[len];
					display.audioIs.read(aud, 0, len);
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}

				Location myPos = display.location.clone().add(display.locEnd).multiply(0.5);

				List<Player> playerList = Bukkit.getOnlinePlayers().stream().filter(player -> player.getLocation().distanceSquared(myPos) <= 256).collect(Collectors.toList());

				if (playerList.isEmpty()) continue;

				List<String> names = playerList.stream().map(player -> player.getName()).collect(Collectors.toList());

				for (Channel webSocket : AudioServer.wsList.keySet()) {
					String s = AudioServer.wsList.get(webSocket);
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
							webSocket.write(new TextWebSocketFrame(pos.getX() + "," + pos.getY() + "," + pos.getZ()));
							webSocket.write(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(aud)));
							webSocket.flush();
						}
					}
				}
			}
		}
	}
}
