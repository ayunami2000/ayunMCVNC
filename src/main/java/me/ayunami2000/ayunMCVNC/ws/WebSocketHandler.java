package me.ayunami2000.ayunMCVNC.ws;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import me.ayunami2000.ayunMCVNC.AudioServer;
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
					conn.disconnect();
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
				if (thePlayer == null) {
					conn.disconnect();
					return;
				}
				String key = RandomStringUtils.randomAlphanumeric(10).toUpperCase();
				AudioServer.authList.put(conn, key);
				AudioServer.nameList.put(key, message);
				thePlayer.spigot().sendMessage(new ComponentBuilder("ayunMCVNC: It seems like you've tried to connect to audio! Please enter this code in the browser to continue: ").append(key).underlined(true).event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, key)).create());
			} else if (msg instanceof CloseWebSocketFrame) {
				AudioServer.wsList.remove(conn);
				String xd = AudioServer.authList.remove(conn);
				if (xd != null) AudioServer.nameList.remove(xd);
			}
		}
	}
}