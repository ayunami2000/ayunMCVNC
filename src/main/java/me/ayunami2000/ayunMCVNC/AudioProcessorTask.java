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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class AudioProcessorTask extends BukkitRunnable {
	private AudioServer ws;

	AudioProcessorTask() {
		if (Main.plugin.httpEnabled) {
			this.ws = new AudioServer(Main.plugin.httpPort);
		}
	}

	public void cleanup() {
		if (this.ws != null) this.ws.stop();
	}

	@Override
	public void run() {
		new Thread(() -> {
			Collection<DisplayInfo> displays = DisplayInfo.displays.values();
			for (DisplayInfo display : displays) {
				if (display.audio < 0) continue;
				byte[] aud;
				try {
					int len = display.audioIs.available();
					if (len <= 0) continue;
					aud = new byte[len];
					display.audioIs.read(aud, 0, len);
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}

				if (System.currentTimeMillis() - display.audioLastWrite > 250) {
					continue;
				}

				if (Main.plugin.audioUdpEnabled) {
					try {
						DatagramPacket dpSend = new DatagramPacket(aud, aud.length, InetAddress.getLoopbackAddress(), display.uniquePort);
						display.audioSocket.send(dpSend);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				if (Main.plugin.httpEnabled) {
					Location myPos = display.location.clone().add(display.locEnd).multiply(0.5);

					List<String> names = Bukkit.getOnlinePlayers().stream().filter(player -> player.hasPermission("ayunmcvnc.view") && DisplayInfo.getNearest(player, 4096) == display).map(player -> player.getName()).collect(Collectors.toList());

					if (names.isEmpty()) continue;

					for (Channel webSocket : AudioServer.wsList.keySet()) {
						String s = AudioServer.wsList.get(webSocket);
						if (names.contains(s)) {
							Player player = Bukkit.getPlayerExact(s);
							Location loc = player.getLocation();
							float yaw = loc.getYaw();
							float pitch = loc.getPitch();
							Vector pos = new Vector(myPos.getX() - loc.getX(), myPos.getY() - loc.getY(), myPos.getZ() - loc.getZ());
							pos = Main.rotateVectorCC(pos, new Vector(0, 1, 0), (float) ((yaw + 180) * Math.PI / 180.0));
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
		}).start();
	}
}
