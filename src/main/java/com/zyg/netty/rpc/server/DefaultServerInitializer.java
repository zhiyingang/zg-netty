package com.zyg.netty.rpc.server;

import com.zyg.netty.rpc.protocol.Decoder;
import com.zyg.netty.rpc.protocol.Encoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class DefaultServerInitializer extends ChannelInitializer<SocketChannel> {


	public DefaultServerInitializer() {

	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		// Create a default pipeline implementation
		final ChannelPipeline p = ch.pipeline();

		p.addLast("decoder", new Decoder(true));

		p.addLast("encoder", new Encoder());

		p.addLast("handler", new DefaultHandler());
		
		p.addLast("httpExceptionHandler", new DefaultExceptionHandler());
	}
}
