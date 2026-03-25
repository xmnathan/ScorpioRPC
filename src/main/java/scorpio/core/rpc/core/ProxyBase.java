package scorpio.core.rpc.core;

import scorpio.core.rpc.common.Param;
import scorpio.core.rpc.common.function.ScoFunction2;
import scorpio.core.rpc.common.function.ScoFunction3;

/**
 * 代理基类
 * 用于生成RPC调用代理
 */
public abstract class ProxyBase {
    /**
     * 异步注册监听返回值
     */
    public abstract void listenResult(ScoFunction2<Param, Param> method, Object... context);
    public abstract void listenResult(ScoFunction3<Boolean, Param, Param> method, Object... context);
    
    /**
     * 同步等待返回值
     */
    public abstract Param waitForResult();
    
    /**
     * 获取函数指针
     */
    public abstract <T> T getMethodFunction(ScorpioService service, int methodKey);
}
