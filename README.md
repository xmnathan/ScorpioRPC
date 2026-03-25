# ScorpioRPC 项目总览
基于Netty实现的RPC通信库,用于替代ZMQ (jeromq) 的RPC通信

## 特性

- ✅ 支持同步/异步调用
- ✅ 支持心跳检测和断线重连
- ✅ 支持批量消息缓冲
- ✅ 支持连接池管理
- ✅ 高性能、低延迟

## 核心组件

- **ScorpioNode**: 节点管理器,管理多个Port,使用Scorpio Server
- **ScorpioRemoteNode**: 远程节点连接,使用Scorpio Client
- **ScorpioPort**: 端口管理器,管理多个Service
- **RPCMessage**: RPC调用消息对象
- **ProxyBase**: 代理基类,用于生成RPC代理
### 1. 核心组件实现

✅ **RPCMessage** - RPC消息对象
- 支持RPC调用、RPC返回、脉冲检测等消息类型
- 兼容Scorpio框架的Call对象

✅ **RPCMessageCodec** - Scorpio消息编解码器
- 使用JSON序列化;兼容Param格式
- 支持自动打包/解包处理
- 支持大消息传输

✅ **ScorpioRemoteNode** - 远程节点连接管理
- 基于Scorpio Client实现
- 支持主动/被动连接
- 支持脉冲检测和自动重连
- 支持连接池管理

✅ **ScorpioPort** - 端口管理器
- 管理多个Service
- 独立线程处理脉冲
- 支持同步/异步调用
- 支持返回值监听

✅ **ScorpioNode** - 节点管理器
- 基于Scorpio Server实现
- 管理多个Port和RemoteNode
- 支持本地/远程消息路由
- 脉冲线程调度

✅ **ScorpioService** - 服务基类
- 抽象服务接口
- 支持函数指针获取
- 支持脉冲覆盖

✅ **ProxyBase** - 代理基类
- 支持异步返回值监听
- 支持同步等待返回值
- 支持函数指针获取

✅ **CallResultBase** - 返回值基类
- 支持超时检测
- 抽象返回值处理

✅ **CallResultAsync** - 异步返回值处理器
- 支持2参和3参回调
- 支持超时处理
- 自动转换返回值为Param

### 2. 工具类实现

✅ **Param** - 参数传递对象
- Map结构存储参数
- 支持泛型get方法
- 兼容Scorpio框架

✅ **ScoFunction0-12** - 函数式接口
- 支持0-12参数的函数
- 用于RPC方法调用

### 3. 文档和示例

✅ **README.md** - 项目介绍
- 特性说明
- 核心组件介绍
- 使用方式

✅ **ExampleService** - 示例服务
- 示例服务定义
- 示例方法实现

✅ **ExampleMain** - 示例主程序
- 示例Node启动
- 示例Service添加
- 示例调用



### 4. 构建配置

✅ **build.gradle** - Gradle构建文件
- 依赖配置
- 编码设置
- JAR打包配置


## 使用方式

### 1. 定义Service

```java
@DistrClass(servId = "testService")
public class TestService extends NettyService {
    
    public void testMethod(String msg) {
        System.out.println("收到消息: " + msg);
        port.returns("success");
    }
}
```

### 2. 创建代理调用

```java
TestServiceProxy proxy = TestServiceProxy.newInstance();
proxy.testMethod("hello");
proxy.listenResult((results, context) -> {
    System.out.println("返回结果: " + results);
});
```
