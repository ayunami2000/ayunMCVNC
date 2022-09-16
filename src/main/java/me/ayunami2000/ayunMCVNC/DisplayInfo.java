package me.ayunami2000.ayunMCVNC;

import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.RTCConfiguration;
import dev.onvoid.webrtc.RTCIceServer;
import dev.onvoid.webrtc.RTCOfferOptions;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.RTCRtpTransceiverDirection;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import dev.onvoid.webrtc.media.audio.AudioOptions;
import dev.onvoid.webrtc.media.audio.AudioTrack;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.DatagramSocket;
import java.net.SocketException;
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
	public ByteArrayOutputStream currentAudio = new ByteArrayOutputStream();
	public DatagramSocket audioSocket;
	public RTCPeerConnection rtcPeerConnection;
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

		uniquePort = (18000 + mapIds.get(0));

		System.out.println(uniquePort);

		try {
			audioSocket = new DatagramSocket();
			audioSocket.setReuseAddress(true);
		} catch (SocketException e) {
			e.printStackTrace();
		}

		PeerConnectionFactory factory = new PeerConnectionFactory();
		RTCConfiguration rtcConfig = new RTCConfiguration();
		RTCIceServer rtcIceServer = new RTCIceServer();
		rtcIceServer.urls.add("stun:stun.l.google.com:19302");
		rtcConfig.iceServers.add(rtcIceServer);
		rtcPeerConnection = factory.createPeerConnection(rtcConfig, rtcIceCandidate -> {
		});
		AudioTrack audioTrack = factory.createAudioTrack("audioTrack", factory.createAudioSource(new AudioOptions()));
		audioTrack.setEnabled(true);
		List<String> fard = new ArrayList<>();
		fard.add("stream");
		rtcPeerConnection.addTrack(audioTrack, fard);
		for (RTCRtpTransceiver transceiver : rtcPeerConnection.getTransceivers()) {
			MediaStreamTrack track = transceiver.getSender().getTrack();

			if (track.getKind().equals(MediaStreamTrack.AUDIO_TRACK_KIND)) {
				transceiver.setDirection(RTCRtpTransceiverDirection.SEND_ONLY);
				break;
			}
		}
		RTCOfferOptions options = new RTCOfferOptions();
		rtcPeerConnection.createOffer(options, new CreateSessionDescriptionObserver() {
			@Override
			public void onSuccess(RTCSessionDescription rtcSessionDescription) {
				rtcPeerConnection.setLocalDescription(rtcSessionDescription, new SetSessionDescriptionObserver() {
					@Override
					public void onSuccess() {
						System.out.println(rtcSessionDescription.sdp); // the big sdp...
					}

					@Override
					public void onFailure(String s) {
						System.err.println(s);
					}

				});
			}

			@Override
			public void onFailure(String s) {
				System.err.println(s);
			}
		});

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
		if (audioSocket != null) {
			audioSocket.disconnect();
			audioSocket.close();
		}
	}

	public void delete() {
		this.delete(false);
	}

	public static DisplayInfo getNearest(CommandSender sender) {
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
			if (minDist > dist) {
				minDist = dist;
				res = display;
			}
		}
		return res;
	}
}
