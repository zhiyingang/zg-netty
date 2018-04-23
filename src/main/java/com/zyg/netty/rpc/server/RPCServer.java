package com.zyg.netty.rpc.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.zyg.netty.rpc.client.proxy.BaseObjectProxy;
import com.zyg.netty.thread.BetterExecutorService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RPCServer {

    private final Logger logger = LoggerFactory.getLogger(RPCServer.class);

	private static HashMap<String,Object> objects =new HashMap<String,Object>();

    private int port;
    private int ioThreadNum;


	public static Object getObject(String objName){
		return objects.get(objName);
	}

	private static BetterExecutorService threadPool;

	public static void submit(Runnable task){
		if(threadPool == null){
			synchronized (BaseObjectProxy.class) {
				if(threadPool==null){
					LinkedBlockingDeque<Runnable> linkedBlockingDeque = new LinkedBlockingDeque<Runnable>();
					ThreadPoolExecutor executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 600L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
					threadPool = new BetterExecutorService(linkedBlockingDeque, executor,"Client async thread pool",100);
				}
			}
		}
		
		threadPool.submit(task);
	}


	
	public RPCServer() throws Exception {

	    this.port = 8080;
	    this.ioThreadNum = 100;
	    
		List<String> objClassList = new ArrayList<>();
		objClassList.add("");
		logger.info("Object list:");
		for( String objClass : objClassList){
			Object obj = RPCServer.class.forName(objClass).newInstance();
			Class[] interfaces= obj.getClass().getInterfaces();
			
			for(int i =0;i<interfaces.length;i++){
				objects.put(interfaces[i].getName(), obj);
				logger.info("   " + interfaces[i].getName());
			}
		}
	}

	public void run() throws Exception {
		final EventLoopGroup bossGroup = new NioEventLoopGroup();
		final EventLoopGroup workerGroup = new NioEventLoopGroup(this.ioThreadNum);
		try {

			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new DefaultServerInitializer())
					.option(ChannelOption.SO_BACKLOG, 10)
					.option(ChannelOption.SO_REUSEADDR, true)
					.option(ChannelOption.SO_KEEPALIVE, true);

			Channel ch = b.bind(port).sync().channel();
			
			logger.info("NettyRPC server listening on port "+ port + " and ready for connections...");
			
	         Runtime.getRuntime().addShutdownHook(new Thread(){
	                @Override
	                public void run(){
	                    
	                    bossGroup.shutdownGracefully();
	                    workerGroup.shutdownGracefully();
	                }
	            });
			ch.closeFuture().sync();

		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception {
		new RPCServer().run();
	}
}
