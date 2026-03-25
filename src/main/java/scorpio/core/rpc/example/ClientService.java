package scorpio.core.rpc.example;

import scorpio.core.rpc.core.ScorpioPort;
import scorpio.core.rpc.core.ScorpioService;

/**
 * 客户端服务
 */
public class ClientService extends ScorpioService {

    public static final String SERV_ID = "clientService";

    public ClientService(ScorpioPort port) {
        super(port);
    }

    /**
     * 调用远程测试消息方法
     */
    public void callRemoteTestMessage(String targetNodeId, String targetPortId, String message) {
        // methodKey: 1 对应 testMessage 方法
        port.call(targetNodeId, targetPortId, ExampleService.SERV_ID, 1, new Object[]{message});
    }

    /**
     * 调用远程测试消息方法 (带返回值监听)
     */
    public void callRemoteTestMessageWithResult(String targetNodeId, String targetPortId, String message, 
                                                  scorpio.core.rpc.common.function.ScoFunction2<scorpio.core.rpc.common.Param, scorpio.core.rpc.common.Param> resultHandler) {
        // methodKey: 1 对应 testMessage 方法
        port.callWithResult(targetNodeId, targetPortId, ExampleService.SERV_ID, 1, new Object[]{message}, resultHandler);
    }

    /**
     * 调用远程加法计算
     */
    public void callRemoteAdd(String targetNodeId, String targetPortId, int a, int b) {
        // methodKey: 2 对应 add 方法
        port.call(targetNodeId, targetPortId, ExampleService.SERV_ID, 2, new Object[]{a, b});
    }

    /**
     * 调用远程加法计算 (带返回值监听)
     */
    public void callRemoteAddWithResult(String targetNodeId, String targetPortId, int a, int b,
                                          scorpio.core.rpc.common.function.ScoFunction2<scorpio.core.rpc.common.Param, scorpio.core.rpc.common.Param> resultHandler) {
        // methodKey: 2 对应 add 方法
        port.callWithResult(targetNodeId, targetPortId, ExampleService.SERV_ID, 2, new Object[]{a, b}, resultHandler);
    }

    /**
     * 调用远程回声测试（带返回值监听）
     */
    public void callRemoteEchoTest(String targetNodeId, String targetPortId, String message) {
        // methodKey: 3 对应 echoTest 方法
        port.call(targetNodeId, targetPortId, ExampleService.SERV_ID, 3, new Object[]{message});
    }

    /**
     * 调用远程回声测试（带返回值监听）
     */
    public void callRemoteEchoTestWithResult(String targetNodeId, String targetPortId, String message,
                                               scorpio.core.rpc.common.function.ScoFunction2<scorpio.core.rpc.common.Param, scorpio.core.rpc.common.Param> resultHandler) {
        // methodKey: 3 对应 echoTest 方法
        port.callWithResult(targetNodeId, targetPortId, ExampleService.SERV_ID, 3, new Object[]{message}, resultHandler);
    }

    @Override
    public Object getId() {
        return SERV_ID;
    }
}
