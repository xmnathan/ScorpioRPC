package scorpio.core.rpc.example;

import scorpio.core.rpc.common.Param;
import scorpio.core.rpc.common.function.*;
import scorpio.core.rpc.core.ProxyBase;
import scorpio.core.rpc.core.ScorpioService;

/**
 * ClientService 代理类
 */
public class ClientServiceProxy extends ProxyBase {
    
    @SuppressWarnings("unchecked")
    public <T> T getMethodFunction(ScorpioService service, int methodKey) {
        ClientService clientService = (ClientService) service;
        
        switch (methodKey) {
            case 1:
                return (T) (ScoFunction3<String, String, String>) clientService::callRemoteTestMessage;
            case 2:
                return (T) (ScoFunction4<String, String, Integer, Integer>) clientService::callRemoteAdd;
            case 3:
                return (T) (ScoFunction3<String, String, String>) clientService::callRemoteEchoTest;
            default:
                throw new IllegalArgumentException("Unknown method key: " + methodKey);
        }
    }
    
    @Override
    public void listenResult(ScoFunction2<Param, Param> method, Object... context) {
    }
    
    @Override
    public void listenResult(ScoFunction3<Boolean, Param, Param> method, Object... context) {
    }
    
    @Override
    public Param waitForResult() {
        return null;
    }
}
