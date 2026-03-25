package scorpio.core.rpc.core;

/**
 * Call返回值基类
 */
public abstract class CallResultBase {
    private final long callId;
    private final long createTime;
    private final long timeout;
    
    public CallResultBase(long callId, long timeout) {
        this.callId = callId;
        this.createTime = System.currentTimeMillis();
        this.timeout = timeout;
    }
    
    /**
     * 处理返回值
     */
    public abstract void onResult(RPCMessage call);
    
    /**
     * 处理超时
     */
    public abstract void onTimeout();
    
    /**
     * 是否超时
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() - createTime > timeout;
    }
    
    /**
     * 获取Call ID
     */
    public long getCallId() {
        return callId;
    }
}
