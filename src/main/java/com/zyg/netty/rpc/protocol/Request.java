package com.zyg.netty.rpc.protocol;


public class Request {
	private long seqNum;
	private int version;
	private byte type;  //0 normal, 1oneway ,2 async
	private byte serializer;//0 kryo 1 json 2 msgpack 3 bson
	
	
	private String objName;
	private String funcName;

	//input
	private Object[] args;

	
	public Request(){
		version = 1;
		type = Constants.RPCType.normal;
		serializer = Constants.RPCSerializer.kryo;
	}
	
	public long getSeqNum() {
		
		return seqNum;
	}

	public void setSeqNum(long seq) {
		this.seqNum = seq;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public byte getType() {
		return type;
	}

	public void setType(byte type) {
		this.type = type;
	}

	public String getObjName() {
		return objName;
	}

	public void setObjName(String objName) {
		this.objName = objName;
	}

	public String getFuncName() {
		return funcName;
	}

	public void setFuncName(String funcName) {
		this.funcName = funcName;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public void setSerializer(byte serializer) {
		this.serializer = serializer;
	}

	public byte getSerializer() {
		return serializer;
	}
}
