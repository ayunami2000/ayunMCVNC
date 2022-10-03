package me.ayunami2000.ayunMCVNC;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public class DisplayInfo {
	public static final Map<String, DisplayInfo> displays = new HashMap<>();

	public static final Set<Integer> screenPartModified = new HashSet<>();

	public static final List<Integer> unusedMapIds = new ArrayList<>();

	public static ZContext directZContext = new ZContext();


	public final List<Integer> mapIds;
	public final String name;
	public boolean mouse;
	public int audio;
	public Location location; // top left corner
	public Location locEnd; // bottom right corner
	public int width;
	public String dest;
	public boolean paused;
	public String directController;
	public boolean altDisplay;

	public BufferedImage currentFrame = null;
	public DatagramSocket audioSocket;
	public int uniquePort;
	public long audioLastWrite = 0;
	public ReentrantLock audioLock = new ReentrantLock();
	public OutputStream audioOs;
	public InputStream audioIs;
	public Process audioProcess;
	public OutputStream directOs;
	public InputStream directIs;
	public Process directProcess;
	public ZMQ.Socket directZSocket;
	public VideoCapture videoCapture;
	private final BukkitTask task1;

	public DisplayInfo(String name, List<Integer> mapIds, boolean mouse, int audio, Location location, int width, String dest, boolean paused, String directController, boolean altDisplay) {
		this.name = name;
		this.mapIds = mapIds;
		this.mouse = mouse;
		this.audio = audio;
		this.location = location;
		this.width = width;
		this.dest = dest;
		this.paused = paused;
		this.altDisplay = altDisplay;

		this.setEndLoc();

		displays.put(this.name, this);

		this.videoCapture = new VideoCapture(this);
		this.videoCapture.start();

		FrameProcessorTask frameProcessorTask = new FrameProcessorTask(this, this.mapIds.size(), this.width);
		Main.tasks.add(task1 = frameProcessorTask.runTaskTimerAsynchronously(Main.plugin, 0, 1));

		switch (Main.plugin.audioUdpPortMode) {
			case 1:
				try {
					ServerSocket ss = new ServerSocket(0);
					uniquePort = ss.getLocalPort();
					ss.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case 0:
			default:
				uniquePort = (18000 + mapIds.get(0));
		}

		System.out.println("Unique port for display " + name + ": " + uniquePort);

		this.setDirectController(directController);

		if (Main.plugin.audioUdpEnabled) {
			try {
				audioSocket = new DatagramSocket();
				audioSocket.setReuseAddress(true);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}

		if (Main.plugin.httpEnabled || Main.plugin.audioUdpEnabled) {
			if (Main.plugin.ffmpegEnabled) {
				try {
					if (Main.plugin.ffmpegParams.isEmpty()) {
						String sampleFormat = Main.plugin.audioSampleFormats.get(Main.plugin.audioSampleFormat);
						try {
							audioProcess = new ProcessBuilder("ffmpeg", "-re", "-hide_banner", "-loglevel", "error", "-fflags", "nobuffer", "-async", Main.plugin.audioFrequency + "", "-thread_queue_size", "4096", "-f", sampleFormat, "-acodec", "pcm_" + sampleFormat, "-ac", Main.plugin.audioChannelNum + "", "-ar", Main.plugin.audioFrequency + "", "-i", "pipe:", "-f", "mp3", "-").start();
						} catch (Exception e) {
							audioProcess = new ProcessBuilder(Main.plugin.getDataFolder().getAbsolutePath() + "/ffmpeg/ffmpeg", "-re", "-hide_banner", "-loglevel", "error", "-fflags", "nobuffer", "-async", Main.plugin.audioFrequency + "", "-thread_queue_size", "4096", "-f", sampleFormat, "-acodec", "pcm_" + sampleFormat, "-ac", Main.plugin.audioChannelNum + "", "-ar", Main.plugin.audioFrequency + "", "-i", "pipe:", "-f", "mp3", "-").start();
						}
					} else {
						audioProcess = new ProcessBuilder(Main.plugin.ffmpegParams).start();
					}
					audioIs = audioProcess.getInputStream();
					audioOs = audioProcess.getOutputStream();
					new Thread(() -> {
						try {
							InputStream err = audioProcess.getErrorStream();
							while (audioProcess.isAlive()) {
								int avail = err.available();
								byte[] errBytes = new byte[avail];
								err.read(errBytes, 0, avail);
								System.out.print(new String(errBytes));
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}).start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				try {
					audioIs = new PipedInputStream();
					audioOs = new PipedOutputStream((PipedInputStream) audioIs);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void setDirectController(String directController) {
		if ((this.directController == null || directController == null) ? (this.directController == null && directController == null) : this.directController.equals(directController)) return;
		if (directProcess != null) {
			directProcess.destroy();
			directIs = null;
			directOs = null;
		}
		try {
			if (directZSocket != null) {
				directZSocket.disconnect("tcp://127.0.0.1:" + uniquePort);
				directZSocket.close();
			}
		} catch (ZMQException ze) {
			ze.printStackTrace();
		}
		this.directController = directController;
		if (directController != null) {
			try {
				try {
					directProcess = new ProcessBuilder("ffmpeg", "-re", "-hide_banner", "-loglevel", "error", "-fflags", "nobuffer", "-f", "image2pipe", "-codec", "mjpeg", "-i", "pipe:", "-vf", "v360=input=flat:output=c6x1:out_forder=lfrbud:yaw=0:pitch=0:rorder=pyr,zmq=bind_address=tcp\\\\://127.0.0.1\\\\:" + uniquePort, "-f", "rawvideo", "-c:v", "mjpeg", "-qscale:v", "16", "-r", "10", "-s", "768x128", "-").start();
				} catch (Exception e) {
					directProcess = new ProcessBuilder(Main.plugin.getDataFolder().getAbsolutePath() + "/ffmpeg/ffmpeg", "-re", "-hide_banner", "-loglevel", "error", "-fflags", "nobuffer", "-f", "image2pipe", "-codec", "mjpeg", "-i", "pipe:", "-vf", "v360=input=flat:output=c6x1:out_forder=lfrbud:yaw=0:pitch=0:rorder=pyr,zmq=bind_address=tcp\\\\://127.0.0.1\\\\:" + uniquePort, "-f", "rawvideo", "-c:v", "mjpeg", "-qscale:v", "16", "-r", "10", "-s", "768x128", "-").start();
				}
				directIs = directProcess.getInputStream();
				directOs = directProcess.getOutputStream();
				directZSocket = directZContext.createSocket(SocketType.REQ);
				directZSocket.connect("tcp://127.0.0.1:" + uniquePort);
				new Thread(() -> {
					try {
						double oldYaw = 0;
						double oldPitch = 0;
						while (directProcess.isAlive()) {
							Player directPlayer = Bukkit.getPlayerExact(this.directController);
							double yaw = oldYaw;
							double pitch = oldPitch;
							if (directPlayer != null && directPlayer.isOnline()) {
								Location loc = directPlayer.getLocation();
								yaw = loc.getYaw();
								pitch = loc.getPitch();
							}
							directZSocket.send("Parsed_v360_0 reset_rot 1");
							directZSocket.recv();
							while (yaw > 180) {
								yaw -= 360;
							}
							while (yaw < -180) {
								yaw += 360;
							}
							directZSocket.send("Parsed_v360_0 yaw " + (-yaw));
							directZSocket.recv();
							while (pitch > 180) {
								pitch -= 360;
							}
							while (pitch < -180) {
								pitch += 360;
							}
							directZSocket.send("Parsed_v360_0 pitch " + pitch);
							directZSocket.recv();
							oldYaw = yaw;
							oldPitch = pitch;
						}
					} catch (ZMQException ze) {
						ze.printStackTrace();
					}
					try {
						if (directZSocket != null) {
							directZSocket.disconnect("tcp://127.0.0.1:" + uniquePort);
							directZSocket.close();
						}
					} catch (ZMQException ze) {
						ze.printStackTrace();
					}
				}).start();
				new Thread(() -> {
					try {
						while (directProcess.isAlive()) {
							int avail = directIs.available();
							if (avail == 0) continue;
							byte[] imgBytes = new byte[avail];
							directIs.read(imgBytes, 0, avail);
							try {
								BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
								if (img != null) currentFrame = img;
							} catch (IIOException e) {
								e.printStackTrace();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}).start();
				new Thread(() -> {
					try {
						InputStream err = directProcess.getErrorStream();
						while (directProcess.isAlive()) {
							int avail = err.available();
							byte[] errBytes = new byte[avail];
							err.read(errBytes, 0, avail);
							System.out.print(new String(errBytes));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static class Shell {

		public String name;
		public List<Integer> mapIds;
		public boolean mouse;
		public int audio;
		public Location location;
		public int width;
		public String dest;
		public boolean paused;
		public String directController;
		public boolean altDisplay;

		public Shell(DisplayInfo source, boolean recycle) {
			this.name = source.name;
			this.mapIds = source.mapIds;
			this.mouse = source.mouse;
			this.audio = source.audio;
			this.location = source.location;
			this.width = source.width;
			this.dest = source.dest;
			this.paused = source.paused;
			this.directController = source.directController;
			this.altDisplay = source.altDisplay;
			source.delete(recycle);
		}

		public DisplayInfo create() {
			return new DisplayInfo(this.name, this.mapIds, this.mouse, this.audio, this.location, this.width, this.dest, this.paused, this.directController, this.altDisplay);
		}
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
			ImageManager.getInstance().recycle();
		}
		if (audioProcess != null) {
			audioProcess.destroy();
		}
		if (directProcess != null) {
			directProcess.destroy();
		}
		try {
			if (directZSocket != null) {
				directZSocket.disconnect("tcp://127.0.0.1:" + uniquePort);
				directZSocket.close();
			}
		} catch (ZMQException ze) {
			ze.printStackTrace();
		}
	}

	public void delete() {
		this.delete(false);
	}

	public static DisplayInfo getNearest(CommandSender sender, int hardLimit) {
		return getNearest(sender, hardLimit, true);
	}

	public static DisplayInfo getNearest(CommandSender sender, int hardLimit, boolean includePaused) {
		List<DisplayInfo> nearestDisplays = getSorted(sender, hardLimit, includePaused, false);
		if (nearestDisplays.isEmpty()) return null;
		return nearestDisplays.get(0);
	}

	public static DisplayInfo getNearest(CommandSender sender) {
		return getNearest(sender, -1, true);
	}

	public static List<DisplayInfo> getSorted(CommandSender sender, int hardLimit, boolean includePaused, boolean includeAll) {
		Collection<DisplayInfo> displayValues = displays.values();
		BlockCommandSender cmdBlockSender = null;
		Player player = null;
		if (sender instanceof BlockCommandSender) {
			cmdBlockSender = (BlockCommandSender) sender;
		} else if (sender instanceof Player) {
			player = (Player) sender;
		} else {
			return new ArrayList<>();
		}
		Set<DisplayInfo> farDisplays = new HashSet<>();
		TreeMap<Double, DisplayInfo> displaySorter = new TreeMap<>();
		Location sourceLoc = (player != null ? player.getLocation() : cmdBlockSender.getBlock().getLocation());
		for (DisplayInfo display : displayValues) {
			if (!includePaused && display.paused) continue;
			if (includeAll) farDisplays.add(display);
			if (sourceLoc.getWorld() != display.location.getWorld()) continue;
			Location targetLoc = display.location.clone().add(display.locEnd).multiply(0.5).add(0.5, 0.5, 0.5);
			double dist = sourceLoc.distanceSquared(targetLoc);
			if (hardLimit != -1 && dist > hardLimit) continue;
			displaySorter.put(dist, display);
			farDisplays.remove(display);
		}
		List<DisplayInfo> resList = new ArrayList<>(displaySorter.values());
		if (includeAll) {
			resList.addAll(farDisplays);
		}
		return resList;
	}
}
