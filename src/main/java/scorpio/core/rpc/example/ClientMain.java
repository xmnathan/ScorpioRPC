package scorpio.core.rpc.example;

import scorpio.core.rpc.core.ScorpioNode;
import scorpio.core.rpc.core.ScorpioPort;


/**
 * 客户端主程序
 */
public class ClientMain {
    
    public static void main(String[] args) {
        // 创建 Node(连接到 ExampleMain 的节点)
        ScorpioNode node = new ScorpioNode("clientNode", "tcp://localhost:8081");
        node.startup();
        
        // 创建 Port
        ScorpioPort port = new ScorpioPort("clientPort0") {
            @Override
            protected void pulseOverride() {
                // 自定义核心逻辑
            }
        };
        port.startup(node);
        
        // 创建并添加 Service
        ClientService clientService = new ClientService(port);
        port.addService(clientService);

        // 主动连接到 ExampleMain 的节点
        node.addRemoteNode("node1", "tcp://localhost:8080");

        System.out.println("Scorpio RPC 客户端启动成功!");
        System.out.println("节点：" + node);
        System.out.println("端口：" + port);

        // 延迟一段时间后发起调用
        try {
            System.out.println("\n等待 2 秒，确保连接建立...");
            Thread.sleep(2000);
            
            System.out.println("\n=== 开始调用远程 ExampleService ===");
            
            // 第 1 次调用：testMessage (使用新的带监听器的 call 方法)
            System.out.println("\n[调用 1] testMessage...");
            clientService.callRemoteTestMessageWithResult("node1", "port0", "Hello from Client!", (results, context) -> {
                System.out.println("✓ [第 1 次] 收到 testMessage 返回结果：" + results);
            });

            // 第 2 次调用：add
            System.out.println("\n[调用 2] add...");
            clientService.callRemoteAddWithResult("node1", "port0", 10, 20, (results, context) -> {
                System.out.println("✓ [第 2 次] 收到 add 返回结果：" + results);
            });


            // 第 3 次调用：echoTest
            System.out.println("\n[调用 3] echoTest...");
            clientService.callRemoteEchoTestWithResult("node1", "port0", "这是一条测试消息", (results, context) -> {
                System.out.println("✓ [第 3 次] 收到 echoTest 返回结果：" + results);
                System.out.println("✓ 通讯测试成功！");
            });
            
            // 等待所有回调执行完成
            System.out.println("\n等待回调执行完成...");
            Thread.sleep(3000);
            
            System.out.println("\n=== 所有测试完成 ===");
            
        } catch (InterruptedException e) {
            System.err.println("主线程被中断：" + e.getMessage());
            e.printStackTrace();
        }
        
        // 保持运行
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 关闭
        node.stop();
    }
}
