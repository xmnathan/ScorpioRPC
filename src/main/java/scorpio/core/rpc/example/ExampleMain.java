package scorpio.core.rpc.example;

import scorpio.core.rpc.core.ScorpioNode;
import scorpio.core.rpc.core.ScorpioPort;


/**
 * 示例主程序
 */
public class ExampleMain {
    
    public static void main(String[] args) {
        // 创建Node
        ScorpioNode node = new ScorpioNode("node1", "tcp://localhost:8080");
        node.startup();
        
        // 创建Port
        ScorpioPort port = new ScorpioPort("port0") {
            @Override
            protected void pulseOverride() {
                // 自定义核心逻辑
            }
        };
        port.startup(node);
        
        // 创建并添加Service
        ExampleService exampleService = new ExampleService(port);
        port.addService(exampleService);
        
        // 模拟调用(实际应该在另一个Node)
        // ExampleServiceProxy proxy = ExampleServiceProxy.newInstance();
        // proxy.testMessage("Hello Scorpio RPC!");
        // proxy.listenResult((results, context) -> {
        //     System.out.println("返回结果: " + results);
        // });
        
        System.out.println("Scorpio RPC示例启动成功!");
        System.out.println("节点: " + node);
        System.out.println("端口: " + port);
        
        // 保持运行
        try {
            Thread.sleep(120000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 关闭
        node.stop();
    }
}
