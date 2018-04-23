package com.zyg.netty.http.channelInitializer;

import com.zyg.netty.http.handler.HttpServerHander;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * Created by yg.zhi on 2018/4/23.
 */
public class HttpChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast("codec",new HttpServerCodec()) //或者使用HttpRequestDecoder & HttpResponseEncoder
            .addLast("aggregator",new HttpObjectAggregator(1024*1024))
            .addLast("handler",new HttpServerHander());
    }
}
