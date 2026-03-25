package scorpio.core.rpc.example;

import scorpio.core.rpc.core.ScorpioPort;
import scorpio.core.rpc.core.ScorpioService;

/**
 * 示例服务
 */
public class ExampleService extends ScorpioService {
    
    public static final String SERV_ID = "exampleService";

    public ExampleService(ScorpioPort port) {
        super(port);
    }

    /**
     * 示例方法: 测试消息
     */
    public void testMessage(String message) {
        System.out.println("收到消息: " + message);
        port.returns("success");
    }
    
    /**
     * 示例方法：计算加法
     */
    public void add(int a, int b) {
        int result = a + b;
        System.out.println("计算：" + a + " + " + b + " = " + result);
        port.returns(result);
    }
    
    /**
     * 示例方法：回声测试（验证通讯）
     */
    public void echoTest(String message) {
        System.out.println("收到 Echo 测试消息：" + message);
        String response = "服务端已收到你的消息：" + message;
        port.returns(response);
    }

    @Override
    public Object getId() {
        return SERV_ID;
    }
}
