package scorpio.core.rpc.core;

/**
 * Scorpio服务基类
 */
public abstract class ScorpioService {
    protected ScorpioPort port;
    
    public ScorpioService(ScorpioPort port) {
        this.port = port;
    }
    
    /**
     * 获取服务ID
     */
    public abstract Object getId();
    
    /**
     * 脉冲
     */
    public void pulse() {
        pulseOverride();
    }
    
    /**
     * 子类可覆盖脉冲
     */
    public void pulseOverride() {
    }
    
    /**
     * 获取RPC函数调用
     */
    public <T> T getMethodFunction(int funcKey) {
        // 通过代理类获取函数引用
        return getProxy().getMethodFunction(this, funcKey);
    }
    
    /**
     * 获取对应的代理类
     */
    private ProxyBase getProxy() {
        try {
            String proxyClassName = getClass().getName() + "Proxy";
            Class<?> proxyClass = Class.forName(proxyClassName);
            return (ProxyBase) proxyClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("创建代理类失败: " + getClass().getName(), e);
        }
    }
    
    /**
     * 获取Port
     */
    public ScorpioPort getPort() {
        return port;
    }
}
