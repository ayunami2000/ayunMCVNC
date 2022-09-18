package me.ayunami2000.ayunMCVNC.ws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import me.ayunami2000.ayunMCVNC.Main;

import java.io.File;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {
	WebSocketServerHandshaker handshaker;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof HttpRequest) {
			HttpRequest httpRequest = (HttpRequest) msg;

			HttpHeaders headers = httpRequest.headers();

			if ("Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION)) &&
					"WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE))) {

				ctx.pipeline().replace(this, "websocketHandler", new WebSocketHandler());

				handleHandshake(ctx, httpRequest);
			} else {
				HttpStaticFileServerHandler httpStaticFileServerHandler = new HttpStaticFileServerHandler(Main.plugin.getDataFolder().getAbsolutePath() + File.separatorChar + "web");
				ctx.pipeline().replace(this, "fileHandler", httpStaticFileServerHandler);
				try {
					httpStaticFileServerHandler.channelRead0(ctx, (HttpRequest) msg);
				} catch (Exception ignored) {
				}
			}
		}
	}

	/* Do the handshaking for WebSocket request */
	protected void handleHandshake(ChannelHandlerContext ctx, HttpRequest req) {
		WebSocketServerHandshakerFactory wsFactory =
				new WebSocketServerHandshakerFactory(getWebSocketURL(req), null, true);
		handshaker = wsFactory.newHandshaker(req);
		if (handshaker == null) {
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
		} else {
			handshaker.handshake(ctx.channel(), req);
		}
	}

	protected String getWebSocketURL(HttpRequest req) {
		String url =  "ws://" + req.headers().get("Host") + req.getUri();
		return url;
	}
}