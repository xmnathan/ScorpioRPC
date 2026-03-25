package scorpio.core.rpc.example;

import scorpio.core.rpc.common.Param;
import scorpio.core.rpc.common.function.*;
import scorpio.core.rpc.core.ProxyBase;
import scorpio.core.rpc.core.ScorpioService;

/**
 * ExampleService 代理类
 */
public class ExampleServiceProxy extends ProxyBase {
    
    @SuppressWarnings("unchecked")
    public <T> T getMethodFunction(ScorpioService service, int methodKey) {
        ExampleService exampleService = (ExampleService) service;
        
        switch (methodKey) {
            case 1:
                return (T) (ScoFunction1<String>) exampleService::testMessage;
            case 2:
                return (T) (ScoFunction2<Integer, Integer>) exampleService::add;
            case 3:
                return (T) (ScoFunction1<String>) exampleService::echoTest;
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
