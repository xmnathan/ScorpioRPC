package scorpio.core.rpc.core;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * RPC消息对象
 * 用于在Netty网络层传输RPC调用请求和返回值
 */
public class RPCMessage {
    // 消息类型
    public static final int TYPE_RPC = 1000;          // RPC远程调用
    public static final int TYPE_RPC_RETURN = 2000;   // RPC远程调用返回
    public static final int TYPE_PING = 4000;         // 脉冲检测
    public static final int TYPE_PONG = 5000;         // 脉冲检测响应
    
    public long id;                      // 请求ID
    public int type;                     // 请求类型
    public String fromNodeId;            // 发送方NodeId
    public String fromPortId;            // 发送方PortId
    public String toNodeId;              // 接收方NodeId
    public String toPortId;              // 接收方PortId
    public String toServId;              // 接收方ServId
    public int methodKey;                // 调用函数key
    public Object[] methodParam;         // 调用函数参数
    public Object[] returns;            // 返回值
    public boolean immutable;            // 是否不可变参数(优化性能)
    
    public RPCMessage() {
    }
    
    /**
     * 创建RPC调用消息
     */
    public static RPCMessage createRPC(long id, String fromNodeId, String fromPortId,
                                       String toNodeId, String toPortId, String toServId,
                                       int methodKey, Object[] methodParam, boolean immutable) {
        RPCMessage msg = new RPCMessage();
        msg.id = id;
        msg.type = TYPE_RPC;
        msg.fromNodeId = fromNodeId;
        msg.fromPortId = fromPortId;
        msg.toNodeId = toNodeId;
        msg.toPortId = toPortId;
        msg.toServId = toServId;
        msg.methodKey = methodKey;
        msg.methodParam = methodParam;
        msg.immutable = immutable;
        return msg;
    }
    
    /**
     * 创建RPC返回消息
     */
    public static RPCMessage createReturn(long id, String fromNodeId, String fromPortId,
                                          String toNodeId, String toPortId,
                                          Object[] returns, boolean immutable) {
        RPCMessage msg = new RPCMessage();
        msg.id = id;
        msg.type = TYPE_RPC_RETURN;
        msg.fromNodeId = fromNodeId;
        msg.fromPortId = fromPortId;
        msg.toNodeId = toNodeId;
        msg.toPortId = toPortId;
        msg.returns = returns;
        msg.immutable = immutable;
        return msg;
    }
    
    /**
     * 创建脉冲检测消息
     */
    public static RPCMessage createPing(String fromNodeId, String toNodeId,
                                        String nodeAddr, boolean main, Object[] methodParam) {
        RPCMessage msg = new RPCMessage();
        msg.type = TYPE_PING;
        msg.fromNodeId = fromNodeId;
        msg.toNodeId = toNodeId;
        msg.methodParam = methodParam;
        msg.immutable = true;
        return msg;
    }
    
    /**
     * 创建脉冲检测响应消息
     */
    public static RPCMessage createPong(String fromNodeId, String toNodeId) {
        RPCMessage msg = new RPCMessage();
        msg.type = TYPE_PONG;
        msg.fromNodeId = fromNodeId;
        msg.toNodeId = toNodeId;
        msg.immutable = true;
        return msg;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("type", type)
                .append("id", id)
                .append("fromNodeId", fromNodeId)
                .append("fromPortId", fromPortId)
                .append("toNodeId", toNodeId)
                .append("toPortId", toPortId)
                .append("toServId", toServId)
                .append("methodKey", methodKey)
                .append("immutable", immutable)
                .toString();
    }
}
