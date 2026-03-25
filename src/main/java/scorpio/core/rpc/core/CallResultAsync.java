package scorpio.core.rpc.core;

import scorpio.core.rpc.common.Param;
import scorpio.core.rpc.common.function.ScoFunction2;
import scorpio.core.rpc.common.function.ScoFunction3;

/**
 * 异步接收返回值
 * 配合ScorpioPort.listenResult()使用
 */
public class CallResultAsync extends CallResultBase {
    private final ScoFunction2<Param, Param> resultMethod;
    private final ScoFunction3<Boolean, Param, Param> resultMethodTimeout;
    private final Param context;
    private final String callerInfo;
    
    public CallResultAsync(long callId, long timeout, 
                          ScoFunction2<Param, Param> resultMethod, Param context) {
        super(callId, timeout);
        this.resultMethod = resultMethod;
        this.resultMethodTimeout = null;
        this.context = context != null ? context : new Param();
        this.callerInfo = this.context.containsKey("_callerInfo") ? this.context.get("_callerInfo") : "Unknown";
    }
    
    public CallResultAsync(long callId, long timeout,
                          ScoFunction3<Boolean, Param, Param> resultMethod, Param context) {
        super(callId, timeout);
        this.resultMethod = null;
        this.resultMethodTimeout = resultMethod;
        this.context = context != null ? context : new Param();
        this.callerInfo = this.context.containsKey("_callerInfo") ? this.context.get("_callerInfo") : "Unknown";
    }
    
    @Override
    public void onResult(RPCMessage call) {
        if (resultMethodTimeout != null) {
            // 3个参数: 超时标志 + 返回值 + 上下文
            resultMethodTimeout.apply(false, convertToParam(call.returns), context);
        } else if (resultMethod != null) {
            // 2个参数: 返回值 + 上下文
            resultMethod.apply(convertToParam(call.returns), context);
        }
    }
    
    @Override
    public void onTimeout() {
        if (resultMethodTimeout != null) {
            resultMethodTimeout.apply(true, new Param(), context);
        }
    }
    
    /**
     * 将返回值数组转换为Param
     */
    private Param convertToParam(Object[] returns) {
        Param param = new Param();
        if (returns != null) {
            for (int i = 0; i < returns.length; i++) {
                param.put("arg" + i, returns[i]);
            }
        }
        return param;
    }
    
    @Override
    public String toString() {
        return "CallResultAsync{callId=" + getCallId() + ", callerInfo=" + callerInfo + "}";
    }
}
