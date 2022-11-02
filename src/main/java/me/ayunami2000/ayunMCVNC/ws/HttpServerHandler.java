package me.ayunami2000.ayunMCVNC.ws;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
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

			if (headers.get("connection") != null && headers.get("connection").toLowerCase().contains("upgrade") && headers.get("upgrade") != null && "WebSocket".equalsIgnoreCase(headers.get("upgrade"))) {

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
			handshakeSafe(handshaker, ctx.channel(), req);
		}
	}

	private Boolean hasHttpReqHandshake = null;

	private void handshakeSafe(WebSocketServerHandshaker handshaker, Channel channel, HttpRequest httpRequest) {
		if (hasHttpReqHandshake == null) {
			try {
				handshaker.handshake(channel, httpRequest);
				hasHttpReqHandshake = true;
				return;
			} catch (NoSuchMethodError e) {
				hasHttpReqHandshake = false;
			}
		}
		if (hasHttpReqHandshake.booleanValue()) {
			handshaker.handshake(channel, httpRequest);
		} else {
			DefaultFullHttpRequest fullRequest = new DefaultFullHttpRequest(httpRequest.getProtocolVersion(), httpRequest.getMethod(), httpRequest.getUri());
			fullRequest.headers().add(httpRequest.headers());
			handshaker.handshake(channel, (FullHttpRequest) fullRequest);
		}
	}

	protected String getWebSocketURL(HttpRequest req) {
		String url =  "ws://" + req.headers().get("Host") + req.getUri();
		return url;
	}
}