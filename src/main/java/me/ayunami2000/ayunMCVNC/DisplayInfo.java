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
	public ZContext directZContext;
	public ZMQ.Socket directZSocket;
	public VideoCapture videoCapture;
	private final BukkitTask task1;

	public DisplayInfo(String name, List<Integer> mapIds, boolean mouse, int audio, Location location, int width, String dest, boolean paused, String directController) {
		this.name = name;
		this.mapIds = mapIds;
		this.mouse = mouse;
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
						audioProcess = new ProcessBuilder("ffmpeg", "-stream_loop", "-1", "-hide_banner", "-loglevel", "error", "-fflags", "nobuffer", "-thread_queue_size", "4096", "-f", sampleFormat, "-acodec", "pcm_" + sampleFormat, "-ac", Main.plugin.audioChannelNum + "", "-ar", Main.plugin.audioFrequency + "", "-i", "pipe:", "-f", "mp3", "-").start();
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
		if ((this.directController == null || directController == null) ? this.directController == directController : this.directController.equals(directController)) return;
		if (directProcess != null) {
			directProcess.destroy();
			directIs = null;
			directOs = null;
		}
		if (directZSocket != null) {
			directZSocket.disconnect("tcp://127.0.0.1:" + uniquePort);
			directZSocket.close();
		}
		if (directZContext != null) {
			if (!directZContext.isClosed()) {
				directZContext.close();
			}
		}
		this.directController = directController;
		if (directController != null) {
			try {
				directProcess = new ProcessBuilder("ffmpeg", "-stream_loop", "-1", "-hide_banner", "-loglevel", "error", "-fflags", "nobuffer", "-thread_queue_size", "4096", "-f", "image2pipe", "-codec", "mjpeg", "-i", "pipe:", "-vf", "v360=input=flat:output=c6x1:out_forder=lfrbud:yaw=0:pitch=0,zmq=bind_address=tcp\\\\://127.0.0.1\\\\:" + uniquePort, "-f", "rawvideo", "-c:v", "mjpeg", "-qscale:v", "16", "-r", "20", "-s", "768x128", "-").start();
				directIs = directProcess.getInputStream();
				directOs = directProcess.getOutputStream();
				directZContext = new ZContext();
				directZSocket = directZContext.createSocket(SocketType.REQ);
				directZSocket.connect("tcp://127.0.0.1:" + uniquePort);
				new Thread(() -> {
					try {
						double yaw = 0;
						double pitch = 0;
						while (directProcess.isAlive() && !directZContext.isClosed()) {
							Player directPlayer = Bukkit.getPlayerExact(this.directController);
							if (directPlayer != null && directPlayer.isOnline()) {
								Location loc = directPlayer.getLocation();
								yaw = loc.getYaw() - 180;
								pitch = loc.getPitch() + 90;
							}
							directZSocket.send("Parsed_v360_0 yaw " + (int) yaw);
							directZSocket.recv();
							directZSocket.send("Parsed_v360_0 pitch " + (int) pitch);
							directZSocket.recv();
						}
					} catch (ZMQException ze) {
						ze.printStackTrace();
					}
					if (!directZContext.isClosed()) {
						directZSocket.disconnect("tcp://127.0.0.1:" + uniquePort);
						directZSocket.close();
						directZContext.close();
					}
				}).start();
				new Thread(() -> {
					try {
						while (directProcess.isAlive()) {
							int avail = directIs.available();
							if (avail == 0) continue;
							byte[] imgBytes = new byte[avail];
							directIs.read(imgBytes, 0, avail);
							BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
							if (img != null) currentFrame = img;
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
			source.delete(recycle);
		}

		public DisplayInfo create() {
			return new DisplayInfo(this.name, this.mapIds, this.mouse, this.audio, this.location, this.width, this.dest, this.paused, this.directController);
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
		if (directZSocket != null) {
			directZSocket.disconnect("tcp://127.0.0.1:" + uniquePort);
			directZSocket.close();
		}
		if (directZContext != null) {
			if (!directZContext.isClosed()) {
				directZContext.close();
			}
		}
	}

	public void delete() {
		this.delete(false);
	}

	public static DisplayInfo getNearest(CommandSender sender, int hardLimit) {
		List<DisplayInfo> nearestDisplays = getSorted(sender, hardLimit);
		if (nearestDisplays.isEmpty()) return null;
		return nearestDisplays.get(0);
	}

	public static DisplayInfo getNearest(CommandSender sender) {
		return getNearest(sender, -1);
	}

	public static List<DisplayInfo> getSorted(CommandSender sender, int hardLimit) {
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
		TreeMap<Double, DisplayInfo> displaySorter = new TreeMap<>();
		for (DisplayInfo display : displayValues) {
			Location sourceLoc = (player != null ? player.getLocation() : cmdBlockSender.getBlock().getLocation());
			if (sourceLoc.getWorld() != display.location.getWorld()) continue;
			Location targetLoc = display.location.clone().add(display.locEnd).multiply(0.5);
			double dist = sourceLoc.distanceSquared(targetLoc);
			if (hardLimit != -1 && dist > hardLimit) continue;
			displaySorter.put(dist, display);
		}
		return new ArrayList<>(displaySorter.values());
	}
}
