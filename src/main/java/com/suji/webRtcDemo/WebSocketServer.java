package com.suji.webRtcDemo;

import java.security.InvalidParameterException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

// implementation based on : 
// https://keyholesoftware.com/2015/03/16/netty-a-different-kind-of-websocket-server/
// https://github.com/jwboardman/khs-stockticker
public class WebSocketServer {

	private static final int DEFAULT_PORT = 9090;
	private final int port;

	public WebSocketServer(int port) {
		this.port = port;
	}

	public static void main(String[] args) throws Exception {
		int port = Integer.parseInt(System.getProperty("WebSocketPort", Integer.toString(DEFAULT_PORT)));
		
		for (int i = 0; i < args.length; ++i) {
			if ("--port".equalsIgnoreCase(args[i])) {
				++i;
				if (i >= args.length)
					throw new InvalidParameterException("--port argument should be followed by an integer value");
				port = Integer.parseInt(args[i]);
			}
			else
				throw new InvalidParameterException("unknown argument: "+args[i]);
		}
		
		WebSocketServer webSocketServer = new WebSocketServer(port);
		webSocketServer.run();
	}

	public void run() {
		final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		final EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			final WsMessageProcessor wsMessageProcessor = new WsMessageProcessor();
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.handler(new LoggingHandler(LogLevel.INFO))
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline p = ch.pipeline();
						p.addLast("decoder", new HttpRequestDecoder());
						p.addLast("aggregator", new HttpObjectAggregator(65536));
						p.addLast("encoder", new HttpResponseEncoder());
						p.addLast("handler", new WebSocketHandler(wsMessageProcessor));
					}
			}).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);

			// Start the server.
			System.out.println("Netty Server started on port: "+port);
			ChannelFuture f = bootstrap.bind(port).sync();

			//wait for the server to close
			f.channel().closeFuture().sync();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

}
