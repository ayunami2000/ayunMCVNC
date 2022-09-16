package me.ayunami2000.ayunMCVNC;

import de.sciss.jump3r.lowlevel.LameEncoder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.java_websocket.WebSocket;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class AudioProcessorTask extends BukkitRunnable {
	private final Object lock = new Object();
	private final AudioServer ws;
	private final AudioFormat audioFormat = new AudioFormat(44100, 16, 2, true, false);
	private final AudioFormat audioFormatOut = new AudioFormat(44100, 16, 2, true, false);

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
				// if (display.currentAudio.size() < (audioFormat.getSampleRate() * audioFormat.getChannels()) / 2) continue; // try to send at least every half-second of audio
				if (display.currentAudio.size() == 0) continue;

				int len = (int) (1000 * display.currentAudio.size() / (audioFormat.getSampleRate() * audioFormat.getChannels()));
				byte[] aud = display.currentAudio.toByteArray();
				display.currentAudio.reset();
				aud = encodePcmToMp3(aud, audioFormat, audioFormatOut);

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
							webSocket.send(pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + len);
							webSocket.send(aud);
						}
					}
				}
			}
		}
	}

	public byte[] encodePcmToMp3(byte[] pcm, AudioFormat inputFormat, AudioFormat outputFormat) {
		LameEncoder encoder = new LameEncoder(inputFormat, outputFormat);

		ByteArrayOutputStream mp3 = new ByteArrayOutputStream();
		byte[] buffer = new byte[encoder.getPCMBufferSize()];

		int bytesToTransfer = Math.min(buffer.length, pcm.length);
		int bytesWritten;
		int currentPcmPosition = 0;
		while (0 < (bytesWritten = encoder.encodeBuffer(pcm, currentPcmPosition, bytesToTransfer, buffer))) {
			currentPcmPosition += bytesToTransfer;
			bytesToTransfer = Math.min(buffer.length, pcm.length - currentPcmPosition);

			mp3.write(buffer, 0, bytesWritten);
		}

		encoder.close();
		return mp3.toByteArray();
	}
}
