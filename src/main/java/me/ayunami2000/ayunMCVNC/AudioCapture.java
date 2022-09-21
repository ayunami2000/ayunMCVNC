package me.ayunami2000.ayunMCVNC;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class AudioCapture extends Thread {
	public boolean running = true;
	private DisplayInfo displayInfo = null;
	private int port;

	private DatagramSocket socket;

	public void onFrame(byte[] frame) {
	}

	public void cleanup() {
		running = false;
		if (socket != null) {
			socket.close();
		}
	}

	public AudioCapture(VideoCaptureBase videoCapture) {
		String[] destPieces = videoCapture.getDestPieces();
		if (!videoCapture.displayInfo.audio || destPieces.length < 2 || destPieces[1].isEmpty()) {
			this.running = false;
			return;
		}
		this.port = Integer.parseInt(destPieces[1]);
		this.displayInfo = videoCapture.displayInfo;
	}

	@Override
	public void run() {
		if (this.displayInfo == null) return;
		while (this.isAlive() && this.running) {
			try {
				byte[] buffer = new byte[4096];
				this.socket = new DatagramSocket(null);
				socket.setReuseAddress(true);
				socket.setSoTimeout(2500);
				socket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), this.port));
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

				while (this.isAlive() && this.running && !displayInfo.paused) {
					this.socket.receive(packet);
					int len = packet.getLength();
					byte[] res = new byte[len];
					System.arraycopy(packet.getData(), packet.getOffset(), res, 0, len);
					onFrame(res);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (socket != null) {
				socket.close();
			}
			if (!this.running) break;
			do {
				try {
					int sleepTime = displayInfo.paused ? 1 : 10;
					for (int i = 0; this.running && i < sleepTime; i++) {
						Thread.sleep(1000);
					}
				} catch (InterruptedException e) {
				}
			} while (displayInfo.paused && this.running);
		}
	}
}
