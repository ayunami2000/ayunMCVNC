package me.ayunami2000.ayunMCVNC.ws;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import me.ayunami2000.ayunMCVNC.AudioServer;
import me.ayunami2000.ayunMCVNC.DisplayInfo;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class WebSocketHandler extends ChannelInboundHandlerAdapter {
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof WebSocketFrame) {
			Channel conn = ctx.channel();
			if (msg instanceof TextWebSocketFrame) {
				String message = ((TextWebSocketFrame) msg).text();
				if (AudioServer.wsList.containsKey(conn)) {
					if (message.equals("keep")) {
						conn.writeAndFlush(new TextWebSocketFrame("alive"));
					} else if (!message.isEmpty()) {
						Player player = Bukkit.getPlayerExact(AudioServer.wsList.get(conn));
						if (player == null || !player.isOnline()) {
							conn.disconnect();
							return;
						}
						if (!player.hasPermission("ayunmcvnc.interact")) {
							return;
						}
						DisplayInfo nearestDisplay = DisplayInfo.getNearest(player, 4096, false);
						if (nearestDisplay == null) {
							return;
						}
						if (message.startsWith("t")) {
							nearestDisplay.videoCapture.typeText(message.substring(1));
						} else if (message.startsWith("k")) {
							try {
								int val = Integer.parseInt(message.substring(1));
								nearestDisplay.videoCapture.pressKey(Math.abs(val), val >= 0);
							} catch (NumberFormatException e) {
								conn.disconnect();
							}
						}
					}
					return;
				}
				if (AudioServer.authList.containsKey(conn)) {
					String key = AudioServer.authList.remove(conn);
					String name = AudioServer.nameList.remove(key);
					message = message.trim();
					if (!message.matches("^[a-zA-Z0-9]{10}$")) {
						conn.disconnect();
						return;
					}
					if (message.equalsIgnoreCase(key)) {
						AudioServer.wsList.put(conn, name);
						conn.writeAndFlush(new TextWebSocketFrame("alive"));
						Player player = Bukkit.getPlayerExact(name);
						if (player == null || !player.isOnline()) {
							conn.disconnect();
							return;
						}
						if (player.hasPermission("ayunmcvnc.interact")) {
							conn.writeAndFlush(new TextWebSocketFrame("input"));
						}
					} else {
						conn.disconnect();
					}
					return;
				}
				message = message.trim();
				if (!message.matches("^[a-zA-Z0-9_]{2,16}$")) {
					conn.disconnect();
					return;
				}
				Player thePlayer = Bukkit.getPlayerExact(message);
				if (thePlayer == null || !thePlayer.hasPermission("ayunmcvnc.view")) {
					conn.disconnect();
					return;
				}
				message = thePlayer.getName();
				String key = RandomStringUtils.randomAlphanumeric(10).toLowerCase();
				AudioServer.authList.put(conn, key);
				AudioServer.nameList.put(key, message);
				conn.writeAndFlush(new TextWebSocketFrame("key"));
				thePlayer.spigot().sendMessage(new ComponentBuilder("ayunMCVNC: It seems like you've tried to connect to audio!\nPlease enter this code in the browser to continue:\n").append(key).underlined(true).event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, key)).create());
			} else if (msg instanceof CloseWebSocketFrame) {
				AudioServer.wsList.remove(conn);
				String xd = AudioServer.authList.remove(conn);
				if (xd != null) AudioServer.nameList.remove(xd);
			}
		}
	}
}