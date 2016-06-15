package com.suji.webRtcDemo;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

//implementation based on : 
//https://keyholesoftware.com/2015/03/16/netty-a-different-kind-of-websocket-server/
//https://github.com/jwboardman/khs-stockticker
public class WebSocketHandler extends SimpleChannelInboundHandler<Object>  {

	private WebSocketServerHandshaker handshaker;
	private WsMessageProcessor wsMessageProcessor;
	private StringBuilder frameBuffer = null;

	public WebSocketHandler(WsMessageProcessor wsMessageProcessor) {
		this.wsMessageProcessor = wsMessageProcessor;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof FullHttpRequest) {
			this.handleHttpRequest(ctx, (FullHttpRequest)msg);
		} else if (msg instanceof WebSocketFrame) {
			this.handleWebSocketFrame(ctx, (WebSocketFrame)msg);
		}
	}

	protected void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
		// Check for closing frame
		if (frame instanceof CloseWebSocketFrame) {
			if (frameBuffer != null) {
				handleMessageCompleted(ctx, frameBuffer.toString());
			}
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			return;
		}

		if (frame instanceof PingWebSocketFrame) {
			ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
			return;
		}

		if (frame instanceof PongWebSocketFrame) {
			return;
		}

		if (frame instanceof TextWebSocketFrame) {
			frameBuffer = new StringBuilder();
			frameBuffer.append(((TextWebSocketFrame)frame).text());
		} else if (frame instanceof ContinuationWebSocketFrame) {
			if (frameBuffer != null) {
				frameBuffer.append(((ContinuationWebSocketFrame)frame).text());
			} 
		} else {
			throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
		}

		// Check if Text or Continuation Frame is final fragment and handle if needed.
		if (frame.isFinalFragment()) {
			handleMessageCompleted(ctx, frameBuffer.toString());
			frameBuffer = null;
		}
	}

	protected void handleMessageCompleted(ChannelHandlerContext ctx, String frameText) {
		wsMessageProcessor.processMessage(ctx, frameText);
	}

	protected boolean handleREST(ChannelHandlerContext ctx, FullHttpRequest req) {
		return false;
	}

	protected void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req)
			throws Exception {
		// Handle a bad request.
		if (!req.decoderResult().isSuccess()) {
			sendHttpErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST);
			return;
		}

		// Allow only GET methods.
		if (req.method() != HttpMethod.GET) {
			sendHttpErrorResponse(ctx, HttpResponseStatus.FORBIDDEN);
			return;
		}

		// check for websocket upgrade request
		String upgradeHeader = req.headers().get("Upgrade");
		if (upgradeHeader != null && "websocket".equalsIgnoreCase(upgradeHeader)) {
			// Handshake. Ideally you'd want to configure your websocket uri
			String url = "ws://" + req.headers().get("Host");
			WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(url, null, false);
			handshaker = wsFactory.newHandshaker(req);
			if (handshaker == null) {
				WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
			} else {
				handshaker.handshake(ctx.channel(), req);
			}
		} else {
			sendHttpErrorResponse(ctx, HttpResponseStatus.FORBIDDEN);
		}
	}

	private void sendHttpErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {
		FullHttpResponse response = new DefaultFullHttpResponse(
				HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

		// Close the connection as soon as the error message is sent.
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

}
