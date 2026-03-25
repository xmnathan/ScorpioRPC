package scorpio.core.rpc.core;

import scorpio.core.rpc.common.Param;
import scorpio.core.rpc.common.function.ScoFunction0;
import scorpio.core.rpc.common.function.ScoFunction1;
import scorpio.core.rpc.common.function.ScoFunction10;
import scorpio.core.rpc.common.function.ScoFunction11;
import scorpio.core.rpc.common.function.ScoFunction12;
import scorpio.core.rpc.common.function.ScoFunction2;
import scorpio.core.rpc.common.function.ScoFunction3;
import scorpio.core.rpc.common.function.ScoFunction4;
import scorpio.core.rpc.common.function.ScoFunction5;
import scorpio.core.rpc.common.function.ScoFunction6;
import scorpio.core.rpc.common.function.ScoFunction7;
import scorpio.core.rpc.common.function.ScoFunction8;
import scorpio.core.rpc.common.function.ScoFunction9;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Scorpio Port
 * 端口管理器;管理多个Service,使用独立线程处理脉冲
 */
public abstract class ScorpioPort {
    private static final Logger log = LoggerFactory.getLogger(ScorpioPort.class);
    
    // 默认异步请求超时时间
    public static final int DEFAULT_TIMEOUT = 60 * 1000;
    
    // 当前线程的Port实例
    private static final ThreadLocal<ScorpioPort> portCurrent = new ThreadLocal<>();
    
    // 所属Node
    private ScorpioNode node;
    // Port名称
    private final String portId;
    
    // 当前时间戳
    private long timeCurrent = 0;
    // 最后发送的请求ID
    private long sendLastCallId = 0;
    
    // 下属服务
    private final Map<Object, ScorpioService> services = new ConcurrentHashMap<>();
    
    // 正在处理中的Call请求(栈)
    private final LinkedList<RPCMessage> callHandling = new LinkedList<>();
    // 接收到待处理的请求
    private final ConcurrentLinkedQueue<RPCMessage> calls = new ConcurrentLinkedQueue<>();
    // 接收到的请求返回值
    private final ConcurrentLinkedQueue<RPCMessage> callResults = new ConcurrentLinkedQueue<>();
    // 本次脉冲需要处理的请求
    private final List<RPCMessage> pulseCalls = new ArrayList<>();
    // 本次脉冲需要处理的返回值
    private final List<RPCMessage> pulseCallResults = new ArrayList<>();
    
    // 请求返回值监听
    private final Map<Long, CallResultBase> callResultListener = new HashMap<>();
    
    /**
     * 构造函数
     */
    public ScorpioPort(String portId) {
        this.portId = portId;
    }
    
    /**
     * 获取当前线程的Port实例
     */
    @SuppressWarnings("unchecked")
    public static <T extends ScorpioPort> T getCurrent() {
        return (T) portCurrent.get();
    }
    
    /**
     * 获取系统时间
     */
    public static long getTime() {
        return getCurrent().getTimeCurrent();
    }
    
    /**
     * 启动
     */
    public void startup(ScorpioNode node) {
        this.node = node;
        this.node.addPort(this);
        log.info("启动Port: {}", this);
    }
    
    /**
     * 结束
     */
    public void stop() {
        node.delPort(this);
        node = null;
    }
    
    /**
     * 脉冲操作
     */
    public final void pulse() {
        timeCurrent = System.currentTimeMillis();
        
        // 确认无脉冲要执行的call和result
        pulseCallAffirm();
        
        // 执行无脉冲的任务
        pulseCalls();
        pulseCallResults();
        
        // 调用下属服务
        pulseServices();
        
        // 调用子类脉冲
        try {
            pulseOverride();
        } catch (Throwable e) {
            log.error("执行pulseOverride错误", e);
        }
    }
    
    /**
     * 确认无脉冲要执行的call和result
     */
    private void pulseCallAffirm() {
        while (!calls.isEmpty()) {
            pulseCalls.add(calls.poll());
        }
        
        while (!callResults.isEmpty()) {
            pulseCallResults.add(callResults.poll());
        }
    }
    
    /**
     * 脉冲中处理请求
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void pulseCalls() {
        while (!pulseCalls.isEmpty()) {
            RPCMessage call = pulseCalls.remove(0);
            
            try {
                // 压入栈
                callHandling.addLast(call);
                
                // 执行Call请求
                ScorpioService serv = getService(call.toServId);
                if (serv == null) {
                    log.warn("执行Call时未找到服务: call={}", call);
                } else {
                    Object f = serv.getMethodFunction(call.methodKey);
                    Object[] m = call.methodParam;
                    
                    // 执行方法
                    switch (call.methodParam.length) {
                        case 0: ((ScoFunction0) f).apply(); break;
                        case 1: ((ScoFunction1) f).apply(m[0]); break;
                        case 2: ((ScoFunction2) f).apply(m[0], m[1]); break;
                        case 3: ((ScoFunction3) f).apply(m[0], m[1], m[2]); break;
                        case 4: ((ScoFunction4) f).apply(m[0], m[1], m[2], m[3]); break;
                        case 5: ((ScoFunction5) f).apply(m[0], m[1], m[2], m[3], m[4]); break;
                        case 6: ((ScoFunction6) f).apply(m[0], m[1], m[2], m[3], m[4], m[5]); break;
                        case 7: ((ScoFunction7) f).apply(m[0], m[1], m[2], m[3], m[4], m[5], m[6]); break;
                        case 8: ((ScoFunction8) f).apply(m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7]); break;
                        case 9: ((ScoFunction9) f).apply(m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8]); break;
                        case 10: ((ScoFunction10) f).apply(m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8], m[9]); break;
                        case 11: ((ScoFunction11) f).apply(m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8], m[9], m[10]); break;
                        case 12: ((ScoFunction12) f).apply(m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8], m[9], m[10], m[11]); break;
                        default: break;
                    }
                }
            } catch (Throwable e) {
                log.error("执行Call错误: call={}", call, e);
            } finally {
                callHandling.removeLast();
            }
        }
    }
    
    /**
     * 脉冲中处理请求返回值
     */
    private void pulseCallResults() {
        while (!pulseCallResults.isEmpty()) {
            try {
                RPCMessage call = pulseCallResults.remove(0);
                
                // 处理返回值
                CallResultBase listener = callResultListener.remove(call.id);
                if (listener != null) {
                    listener.onResult(call);
                } else {
                    log.error("处理返回值时未找到接收对象: call={}", call);
                }
            } catch (Throwable e) {
                log.error("处理返回值错误", e);
            }
        }
    }
    
    /**
     * 调用下属服务的脉冲
     */
    private void pulseServices() {
        for (ScorpioService service : services.values()) {
            try {
                service.pulse();
            } catch (Throwable e) {
                log.error("Service pulse错误: service={}", service.getId(), e);
            }
        }
    }
    
    /**
     * 发起远程调用 RPC 请求
     */
    public void call(String toNodeId, String toPortId, String toServId, 
                      int methodKey, Object[] methodParam) {
        call(false, toNodeId, toPortId, toServId, methodKey, methodParam);
    }
    
    /**
     * 发起远程调用 RPC 请求 (带返回值监听)
     */
    public void callWithResult(String toNodeId, String toPortId, String toServId,
                                int methodKey, Object[] methodParam,
                                ScoFunction2<Param, Param> resultHandler) {
        callWithResult(false, toNodeId, toPortId, toServId, methodKey, methodParam, resultHandler);
    }
    
    /**
     * 发起远程调用 RPC 请求 (带返回值监听)
     */
    public void callWithResult(boolean immutable, String toNodeId, String toPortId, String toServId,
                                int methodKey, Object[] methodParam,
                                ScoFunction2<Param, Param> resultHandler) {
        // 先申请 ID
        long callId = applyCallId();
        
        // 注册监听器
        CallResultBase crb = new CallResultAsync(callId, DEFAULT_TIMEOUT, resultHandler, null);
        callResultListener.put(callId, crb);
        
        // 创建并发送消息
        RPCMessage msg = RPCMessage.createRPC(
                callId,
                node.getId(),
                portId,
                toNodeId,
                toPortId,
                toServId,
                methodKey,
                methodParam,
                immutable
        );
        
        sendMessage(msg);
    }
    
    /**
     * 发起远程调用RPC请求
     */
    public void call(boolean immutable, String toNodeId, String toPortId, String toServId,
                      int methodKey, Object[] methodParam) {
        RPCMessage msg = RPCMessage.createRPC(
                applyCallId(),
                node.getId(),
                portId,
                toNodeId,
                toPortId,
                toServId,
                methodKey,
                methodParam,
                immutable
        );
        
        sendMessage(msg);
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(RPCMessage msg) {
        if (node.getId().equals(msg.toNodeId)) {
            // 本Node直接处理
            handleLocalMessage(msg);
        } else {
            // 发送到远程Node
            node.sendRemoteMessage(msg);
        }
    }
    
    /**
     * 处理本地消息
     */
    private void handleLocalMessage(RPCMessage msg) {
        ScorpioPort port = node.getPort(msg.toPortId);
        if (port != null) {
            if (msg.type == RPCMessage.TYPE_RPC) {
                port.addCall(msg);
            } else if (msg.type == RPCMessage.TYPE_RPC_RETURN) {
                port.addCallResult(msg);
            }
        } else {
            log.warn("处理本地消息时未找到Port: msg={}", msg);
        }
    }
    
    /**
     * 申请请求ID
     */
    private long applyCallId() {
        return ++sendLastCallId;
    }
    
    /**
     * 监听返回值
     */
    public void listenResult(ScoFunction2<Param, Param> method, Object... context) {
        listenResult(method, new Param(context));
    }
    
    public void listenResult(ScoFunction3<Boolean, Param, Param> method, Object... context) {
        listenResult(method, new Param(context));
    }
    
    public void listenResult(ScoFunction2<Param, Param> method, Param context) {
        CallResultBase crb = new CallResultAsync(sendLastCallId, DEFAULT_TIMEOUT, method, context);
        callResultListener.put(sendLastCallId, crb);
    }
    
    public void listenResult(ScoFunction3<Boolean, Param, Param> method, Param context) {
        CallResultBase crb = new CallResultAsync(sendLastCallId, DEFAULT_TIMEOUT, method, context);
        callResultListener.put(sendLastCallId, crb);
    }
    
    /**
     * 获取服务
     */
    @SuppressWarnings("unchecked")
    public <T extends ScorpioService> T getService(Object id) {
        return (T) services.get(id);
    }
    
    /**
     * 添加服务
     */
    public void addService(ScorpioService service) {
        services.put(service.getId(), service);
        log.info("添加服务: nodeId={}, portId={}, service={}", 
                getNodeId(), getId(), service.getId());
    }
    
    /**
     * 删除服务
     */
    public void delService(Object id) {
        ScorpioService serv = services.get(id);
        log.info("删除服务: nodeId={}, portId={}, service={}", 
                getNodeId(), getId(), serv != null ? serv.getId() : id);
        services.remove(id);
    }
    
    /**
     * 添加待处理请求
     */
    public void addCall(RPCMessage call) {
        calls.add(call);
    }
    
    /**
     * 添加待处理请求返回值
     */
    public void addCallResult(RPCMessage call) {
        callResults.add(call);
    }
    
    /**
     * 发送返回值
     */
    public void returns(Object... values) {
        RPCMessage call = getCall();
        RPCMessage returnMsg = RPCMessage.createReturn(
                call.id,
                node.getId(),
                portId,
                call.fromNodeId,
                call.fromPortId,
                values,
                call.immutable
        );
        
        sendMessage(returnMsg);
    }
    
    /**
     * 子类可覆盖脉冲
     */
    protected void pulseOverride() {
    }
    
    // Getters
    public ScorpioNode getNode() {
        return node;
    }
    
    public String getNodeId() {
        return node.getId();
    }
    
    public String getId() {
        return portId;
    }
    
    public long getTimeCurrent() {
        return timeCurrent;
    }
    
    public RPCMessage getCall() {
        return callHandling.getLast();
    }
    
    @Override
    public String toString() {
        return "Port{nodeId=" + getNodeId() + ", portId=" + getId() + "}";
    }
}
