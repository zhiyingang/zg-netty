package com.zyg.netty.rpc.client;

import com.zyg.netty.rpc.client.proxy.AsyncRPCCallback;
import com.zyg.netty.rpc.protocol.Constants;
import com.zyg.netty.rpc.protocol.RPCContext;
import com.zyg.netty.rpc.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;


public class RPCFuture implements Future<Object>{
    private final Logger logger = LoggerFactory.getLogger(RPCFuture.class);
    
    private Sync sync;
	private RPCContext rpcCtx;
	
	private ReentrantLock lock = new ReentrantLock();
	private List<AsyncRPCCallback> pendingCallbacks = new ArrayList<AsyncRPCCallback>();

	private DefaultClientHandler handler;
    private long startTime;
    private long responseTimeThreshold = 300;
    
    static class Sync extends AbstractQueuedSynchronizer {
  
		private static final long serialVersionUID = 1L;
		
		//future status
		private final int done = 1;
		private final int pending = 0;

		protected boolean tryAcquire(int acquires) {
            return getState()==done?true:false;
        }

        protected  boolean tryRelease(int releases) {
            if (getState() == pending) {
                if (compareAndSetState(pending, 1)) {
                    return true;
                }
            }
			return false;
        }
        
        public boolean isDone(){
        	getState();
			return getState()==done;
        }
    }


//	//Constructor
//	public RPCFuture (RPCContext rpcCtx,DefaultClientHandler handler, AsyncRPCCallback callback){
//		this.sync = new Sync();
//		this.rpcCtx = rpcCtx;
//		this.handler = handler;
//		this.callback = callback;
//	}
	
	//Constructor
	public RPCFuture (RPCContext rpcCtx,DefaultClientHandler handler){
		this.sync = new Sync();
		this.rpcCtx = rpcCtx;
		this.handler = handler;
		this.startTime = System.currentTimeMillis();
	}
	
	@Override
	public boolean isDone() {
		return sync.isDone();
	}
	
	@Override
	public Object get() throws InterruptedException, ExecutionException {
		sync.acquire(-1);
		return processResponse();
	}


	@Override
	public Object get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));

		if(success){
			return processResponse();
		}else{
			throw new RuntimeException("Timeout exception|objName="+rpcCtx.getRequest().getObjName()+"|funcName="+rpcCtx.getRequest().getFuncName());
		}
	}

	@Override
	public boolean isCancelled() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		throw new UnsupportedOperationException();
	}
	
	
	//wake up caller thread or summit task to excute async callback , will be called by event loop thread when received response from Server.
	public void done(Response reponse){
		this.rpcCtx.setResponse(reponse);
		byte type = rpcCtx.getRequest().getType();
		if(type== Constants.RPCType.normal){//wake up caller thread 
			sync.release(1);
		}else if(type== Constants.RPCType.async ){//submit task to execute async callback
			sync.release(1);
			invokeCallbacks();

		}else if(type== Constants.RPCType.oneway){
			//oneway call wonn't got a response from server.
			
		}
		//Threshold
		long responseTime = System.currentTimeMillis() - startTime;
		if(responseTime > this.responseTimeThreshold){
		    logger.warn("Service response time is too slow|serviceName="+reponse.getObjName()+"|funcName="+reponse.getFuncName()+"|responseTime=" + responseTime);
		}
	}
	
	private void invokeCallbacks(){
	    lock.lock();
        try{
            for(final AsyncRPCCallback callback : pendingCallbacks){
                runCallback(callback);
            }
        }finally{
            lock.unlock();
        }
	}
	
	private void runCallback(final AsyncRPCCallback callback){
        RPCClient.getInstance().submit(new Runnable(){
            @Override
            public void run() {
                Response response = rpcCtx.getResponse();
                char status = response.getStatus();
                if(status == Constants.RPCStatus.ok){
                    callback.success(response.getResult());
                }else if(status == Constants.RPCStatus.exception){
                    callback.fail(new RuntimeException("Got exception in server|objName="+rpcCtx.getRequest().getObjName()+"|funcName="+rpcCtx.getRequest().getFuncName()+"|server msg="+response.getMsg()));
                }else if(status == Constants.RPCStatus.unknownError){
                    callback.fail(new RuntimeException("Got unknown error in server|objName="+rpcCtx.getRequest().getObjName()+"|funcName="+rpcCtx.getRequest().getFuncName()+"|server msg="+response.getMsg()));
                }
            }
        });
	}
	
	public RPCFuture addCallback(AsyncRPCCallback callback){
	    lock.lock();
	    try{
	        if(isDone()){
	            runCallback(callback);
	        }else{
	            this.pendingCallbacks.add(callback);
	        }
	    }finally{
	        lock.unlock();
	    }
	    return this;
	}
	

    //call by caller thread to get result
    private Object processResponse() {

        byte type = rpcCtx.getRequest().getType();
        
        if(type == Constants.RPCType.normal||type == Constants.RPCType.async){//process response to return result or throw error/exception.
            
            Response response = rpcCtx.getResponse();
            char status = response.getStatus();
            
            if(status == Constants.RPCStatus.exception){
                throw new RuntimeException("Got exception in server|objName="+rpcCtx.getRequest().getObjName()+"|funcName="+rpcCtx.getRequest().getFuncName()+"|server msg="+response.getMsg());
            }else if(status == Constants.RPCStatus.unknownError){
                throw new RuntimeException("Got unknown error in server|objName="+rpcCtx.getRequest().getObjName()+"|funcName="+rpcCtx.getRequest().getFuncName()+"|server msg="+response.getMsg());
            }
        }
        return rpcCtx.getResponse().getResult();
    }
}
