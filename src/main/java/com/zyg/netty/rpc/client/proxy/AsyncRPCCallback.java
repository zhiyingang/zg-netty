package com.zyg.netty.rpc.client.proxy;

public interface AsyncRPCCallback {
	
	public void success(Object result);
	
	public void fail(Exception e);
	
}
