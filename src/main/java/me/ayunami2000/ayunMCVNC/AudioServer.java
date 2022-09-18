package me.ayunami2000.ayunMCVNC;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import me.ayunami2000.ayunMCVNC.ws.HTTPInitializer;

import java.util.HashMap;

public class AudioServer {
	public static final HashMap<Channel, String> wsList = new HashMap<>();
	public static HashMap<Channel, String> authList = new HashMap<>();
	public static HashMap<String, String> nameList = new HashMap<>();

	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;

	public AudioServer(int port) {
		bossGroup = new NioEventLoopGroup(1);
		workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.option(ChannelOption.SO_REUSEADDR, true);
			b.option(ChannelOption.SO_BACKLOG, 1024);
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new HTTPInitializer());

			b.bind(port).sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}
}
