package com.zyg.netty.rpc.client.proxy;


import com.zyg.netty.rpc.client.DefaultClientHandler;
import com.zyg.netty.rpc.client.RPCFuture;
import com.zyg.netty.rpc.protocol.Constants;
import com.zyg.netty.rpc.protocol.RPCContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ObjectProxy<T> extends BaseObjectProxy<T> implements InvocationHandler,IAsyncObjectProxy {

    public ObjectProxy(Class<T> clazz){
        super(clazz);
    }
    
	public ObjectProxy(List<InetSocketAddress> servers, Class<T> clazz){
		super(servers, clazz);
	}

	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		   if(Object.class  == method.getDeclaringClass()) {
		       String name = method.getName();
		       if("equals".equals(name)) {
		           return proxy == args[0];
		       } else if("hashCode".equals(name)) {
		           return System.identityHashCode(proxy);
		       } else if("toString".equals(name)) {
		           return proxy.getClass().getName() + "@" +
		               Integer.toHexString(System.identityHashCode(proxy)) +
		               ", with InvocationHandler " + this;
		       } else {
		           throw new IllegalStateException(String.valueOf(method));
		       }
		   }

		   DefaultClientHandler handler = chooseHandler();
		   long seqNum = handler.getNextSequentNumber();
		   RPCContext rpcCtx = createRequest(method.getName(), args, seqNum, Constants.RPCType.normal);

		   RPCFuture rpcFuture = handler.doRPC(rpcCtx);
		   return rpcFuture.get(this.syncCallTimeOutMillis, TimeUnit.MILLISECONDS);
	}

	@Override
	public RPCFuture call(String funcName, Object... args){
		
		DefaultClientHandler handler = chooseHandler();
		long seqNum = handler.getNextSequentNumber();
		RPCContext rpcCtx = createRequest(funcName, args, seqNum, Constants.RPCType.async);

		RPCFuture rpcFuture = handler.doRPC(rpcCtx);
		return rpcFuture;
	}
	
	@Override
	public void notify(String funcName, Object[] args) {
		
		DefaultClientHandler handler = chooseHandler();
		long seqNum = handler.getNextSequentNumber();
		RPCContext rpcCtx = createRequest(funcName, args, seqNum, Constants.RPCType.oneway);

		handler.doNotify(rpcCtx);
	}
	
}
