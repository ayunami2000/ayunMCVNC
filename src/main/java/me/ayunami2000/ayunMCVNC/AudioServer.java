package me.ayunami2000.ayunMCVNC;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class AudioServer extends WebSocketServer {
	public HashMap<WebSocket, String> wsList = new HashMap<>();

	public AudioServer(InetSocketAddress address) {
		super(address);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {

	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		wsList.remove(conn);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		if (wsList.containsKey(conn)) {
			conn.close();
		} else {
			wsList.put(conn, message);
		}
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {

	}

	@Override
	public void onError(WebSocket conn, Exception ex) {

	}

	@Override
	public void onStart() {

	}
}