package me.ayunami2000.ayunMCVNC;

import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.PeerConnectionObserver;
import dev.onvoid.webrtc.RTCAnswerOptions;
import dev.onvoid.webrtc.RTCConfiguration;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceConnectionState;
import dev.onvoid.webrtc.RTCIceGatheringState;
import dev.onvoid.webrtc.RTCIceServer;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.RTCRtpTransceiverDirection;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import dev.onvoid.webrtc.media.audio.AudioDeviceModule;
import dev.onvoid.webrtc.media.audio.AudioOptions;
import dev.onvoid.webrtc.media.audio.AudioSink;
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
import java.util.Base64;
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

		AudioDeviceModule module = new AudioDeviceModule();
		AudioSink sink = new AudioSink() {

			@Override
			public void onRecordedData(byte[] audioSamples, int nSamples,
									   int nBytesPerSample, int nChannels, int samplesPerSec,
									   int totalDelayMS, int clockDrift) {

			}
		};

		module.setRecordingDevice(MediaDevices.getDefaultAudioCaptureDevice());
		module.setAudioSink(sink);
		module.initRecording();

		PeerConnectionFactory factory = new PeerConnectionFactory(module);
		RTCConfiguration rtcConfig = new RTCConfiguration();
		RTCIceServer rtcIceServer = new RTCIceServer();
		rtcIceServer.urls.add("stun:stun.l.google.com:19302");
		rtcConfig.iceServers.add(rtcIceServer);
		RTCPeerConnection rtcPeerConnection = factory.createPeerConnection(rtcConfig, new PeerConnectionObserver() {
			@Override
			public void onIceCandidate(RTCIceCandidate rtcIceCandidate) {

			}

			@Override
			public void onIceConnectionChange(RTCIceConnectionState state) {
				System.out.println(state.name());
			}
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

		rtcPeerConnection.setRemoteDescription(new RTCSessionDescription(RTCSdpType.OFFER, new String(Base64.getDecoder().decode("eyJ0eXBlIjoib2ZmZXIiLCJzZHAiOiJ2PTBcclxubz1tb3ppbGxhLi4uVEhJU19JU19TRFBBUlRBLTk5LjAgNDQ2MTg2MTk3MTUyOTA3MjQwNCAwIElOIElQNCAwLjAuMC4wXHJcbnM9LVxyXG50PTAgMFxyXG5hPXNlbmRyZWN2XHJcbmE9ZmluZ2VycHJpbnQ6c2hhLTI1NiAxRjpEQTozQzo2MDo4RDoxMjo0OTo2NzpFOTowNzoyODozRTo3RTo2NTo1NDo0RDpCNzpGMzo3MjoxNDozRTo5RDoyMjo3MTpDNzpGMjoxMDo3NTozRDpDRDowMzo0QVxyXG5hPWdyb3VwOkJVTkRMRSAwXHJcbmE9aWNlLW9wdGlvbnM6dHJpY2tsZVxyXG5hPW1zaWQtc2VtYW50aWM6V01TICpcclxubT1hdWRpbyA2MzM5MSBVRFAvVExTL1JUUC9TQVZQRiAxMDkgOSAwIDggMTAxXHJcbmM9SU4gSVA0IDE3NC4xMDkuMzkuMjZcclxuYT1jYW5kaWRhdGU6MCAxIFVEUCAyMTIyMTg3MDA3IGE5ZTk3YzYxLTMyOTEtNDk4Ny04MGExLTYyYTE5MDdjNGM4ZS5sb2NhbCA2MzM5MSB0eXAgaG9zdFxyXG5hPWNhbmRpZGF0ZToyIDEgVURQIDIxMjIyNTI1NDMgZTAwNGVkOGItMDAyYi00MThkLThiYTAtM2M2ZjU5MDNiYmQ1LmxvY2FsIDYzMzkyIHR5cCBob3N0XHJcbmE9Y2FuZGlkYXRlOjQgMSBUQ1AgMjEwNTQ1ODk0MyBhOWU5N2M2MS0zMjkxLTQ5ODctODBhMS02MmExOTA3YzRjOGUubG9jYWwgOSB0eXAgaG9zdCB0Y3B0eXBlIGFjdGl2ZVxyXG5hPWNhbmRpZGF0ZTo1IDEgVENQIDIxMDU1MjQ0NzkgZTAwNGVkOGItMDAyYi00MThkLThiYTAtM2M2ZjU5MDNiYmQ1LmxvY2FsIDkgdHlwIGhvc3QgdGNwdHlwZSBhY3RpdmVcclxuYT1jYW5kaWRhdGU6MCAyIFVEUCAyMTIyMTg3MDA2IGE5ZTk3YzYxLTMyOTEtNDk4Ny04MGExLTYyYTE5MDdjNGM4ZS5sb2NhbCA2MzM5MyB0eXAgaG9zdFxyXG5hPWNhbmRpZGF0ZToyIDIgVURQIDIxMjIyNTI1NDIgZTAwNGVkOGItMDAyYi00MThkLThiYTAtM2M2ZjU5MDNiYmQ1LmxvY2FsIDYzMzk0IHR5cCBob3N0XHJcbmE9Y2FuZGlkYXRlOjQgMiBUQ1AgMjEwNTQ1ODk0MiBhOWU5N2M2MS0zMjkxLTQ5ODctODBhMS02MmExOTA3YzRjOGUubG9jYWwgOSB0eXAgaG9zdCB0Y3B0eXBlIGFjdGl2ZVxyXG5hPWNhbmRpZGF0ZTo1IDIgVENQIDIxMDU1MjQ0NzggZTAwNGVkOGItMDAyYi00MThkLThiYTAtM2M2ZjU5MDNiYmQ1LmxvY2FsIDkgdHlwIGhvc3QgdGNwdHlwZSBhY3RpdmVcclxuYT1jYW5kaWRhdGU6MSAxIFVEUCAxNjg1OTg3MzI3IDE3NC4xMDkuMzkuMjYgNjMzOTEgdHlwIHNyZmx4IHJhZGRyIDAuMC4wLjAgcnBvcnQgMFxyXG5hPWNhbmRpZGF0ZToxIDIgVURQIDE2ODU5ODczMjYgMTc0LjEwOS4zOS4yNiA2MzM5MyB0eXAgc3JmbHggcmFkZHIgMC4wLjAuMCBycG9ydCAwXHJcbmE9c2VuZHJlY3ZcclxuYT1lbmQtb2YtY2FuZGlkYXRlc1xyXG5hPWV4dG1hcDoxIHVybjppZXRmOnBhcmFtczpydHAtaGRyZXh0OnNzcmMtYXVkaW8tbGV2ZWxcclxuYT1leHRtYXA6Mi9yZWN2b25seSB1cm46aWV0ZjpwYXJhbXM6cnRwLWhkcmV4dDpjc3JjLWF1ZGlvLWxldmVsXHJcbmE9ZXh0bWFwOjMgdXJuOmlldGY6cGFyYW1zOnJ0cC1oZHJleHQ6c2RlczptaWRcclxuYT1mbXRwOjEwOSBtYXhwbGF5YmFja3JhdGU9NDgwMDA7c3RlcmVvPTE7dXNlaW5iYW5kZmVjPTFcclxuYT1mbXRwOjEwMSAwLTE1XHJcbmE9aWNlLXB3ZDoyOTYxNDBhYTVjMGM2NzJkYzAxMmRhMDYyODg2YzZiNVxyXG5hPWljZS11ZnJhZzpiMjgwNmJhNVxyXG5hPW1pZDowXHJcbmE9bXNpZDotIHtkZjJhNDY2ZC0yY2EyLTRkNTktODEyNi0yNWZhMDE4MzRhYjJ9XHJcbmE9cnRjcDo2MzM5MyBJTiBJUDQgMTc0LjEwOS4zOS4yNlxyXG5hPXJ0Y3AtbXV4XHJcbmE9cnRwbWFwOjEwOSBvcHVzLzQ4MDAwLzJcclxuYT1ydHBtYXA6OSBHNzIyLzgwMDAvMVxyXG5hPXJ0cG1hcDowIFBDTVUvODAwMFxyXG5hPXJ0cG1hcDo4IFBDTUEvODAwMFxyXG5hPXJ0cG1hcDoxMDEgdGVsZXBob25lLWV2ZW50LzgwMDBcclxuYT1zZXR1cDphY3RwYXNzXHJcbmE9c3NyYzoxNjIyNDU2NDY3IGNuYW1lOnszNjcxMzM5MS0yMDdlLTQ1N2EtOWQxZi0wNTQ2MjJkMzA0OGZ9XHJcbiJ9"))), new SetSessionDescriptionObserver() {
			@Override
			public void onSuccess() {
				rtcPeerConnection.createAnswer(new RTCAnswerOptions(), new CreateSessionDescriptionObserver() {
					@Override
					public void onSuccess(RTCSessionDescription rtcSessionDescription) {
						rtcPeerConnection.setLocalDescription(rtcSessionDescription, new SetSessionDescriptionObserver() {
							@Override
							public void onSuccess() {
								while (!rtcPeerConnection.getIceGatheringState().equals(RTCIceGatheringState.COMPLETE)) {

								}
								System.out.println(Base64.getEncoder().encodeToString(rtcPeerConnection.getLocalDescription().sdp.getBytes()));
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
