package scorpio.core.rpc.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Scorpio Node
 * 节点管理器;管理多个Port,使用Scorpio Server接收连接
 */
public final class ScorpioNode {
    private static final Logger log = LoggerFactory.getLogger(ScorpioNode.class);
    
    private final String id;                                // Node名称
    private String partId = "";                            // 内部ID
    private final String addr;                             // Node地址
    
    private final ConcurrentHashMap<String, ScorpioPort> ports = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScorpioRemoteNode> remoteNodes = new ConcurrentHashMap<>();
    
    // Scorpio Server
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    // 脉冲定时器
    private volatile boolean running = false;
    private final Object pulseLock = new Object();
    
    /**
     * 构造函数
     */
    public ScorpioNode(String id, String addr) {
        this.id = id;
        this.addr = addr;
    }
    
    public ScorpioNode(String id, String addr, String partId) {
        this(id, addr);
        this.partId = partId;
    }
    
    /**
     * 启动Node
     */
    public void startup() {
        try {
            // 启动Scorpio Server
            startServer();
            
            // 启动脉冲线程
            startPulseThread();
            
            running = true;
            log.info("Node启动成功: {}", this);
            
        } catch (Exception e) {
            log.error("Node启动失败: {}", this, e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 启动Scorpio Server
     */
    private void startServer() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        // 脉冲检测
                        pipeline.addLast(new IdleStateHandler(ScorpioRemoteNode.INTERVAL_PING / 1000, 
                                0, 0, TimeUnit.SECONDS));
                        
                        // 消息编解码
                        pipeline.addLast(new RPCMessageCodec());
                        
                        // 消息处理器
                        pipeline.addLast(new ScorpioNodeServerHandler(ScorpioNode.this));
                    }
                });
        
        // 解析地址
        String[] addrParts = addr.split(":");
        int port = Integer.parseInt(addrParts[addrParts.length - 1]);
        
        // 绑定端口
        ChannelFuture future = bootstrap.bind(port).sync();
        serverChannel = future.channel();
        
        log.info("Scorpio Server启动成功: addr={}", addr);
    }
    
    /**
     * 启动脉冲线程
     */
    private void startPulseThread() {
        Thread pulseThread = new Thread(() -> {
            while (running) {
                try {
                    synchronized (pulseLock) {
                        // 调用所有Port的脉冲
                        for (ScorpioPort port : ports.values()) {
                            port.pulse();
                        }
                        
                        // 调用所有RemoteNode的脉冲
                        long now = System.currentTimeMillis();
                        for (ScorpioRemoteNode remote : remoteNodes.values()) {
                            remote.pulse(now);
                        }
                    }
                    
                    Thread.sleep(100); // 100ms脉冲间隔
                    
                } catch (InterruptedException e) {
                    if (running) {
                        log.error("脉冲线程异常", e);
                    }
                } catch (Exception e) {
                    log.error("脉冲执行异常", e);
                }
            }
        });
        
        pulseThread.setName("PulseThread-" + id);
        pulseThread.setDaemon(true);
        pulseThread.start();
        
        log.info("脉冲线程启动成功: {}", id);
    }
    
    /**
     * 停止Node
     */
    public void stop() {
        running = false;
        
        // 关闭所有Port
        for (ScorpioPort port : ports.values()) {
            port.stop();
        }
        ports.clear();
        
        // 关闭所有RemoteNode
        for (ScorpioRemoteNode remote : remoteNodes.values()) {
            remote.close();
        }
        remoteNodes.clear();
        
        // 关闭Scorpio Server
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        log.info("Node停止: {}", this);
    }
    
    /**
     * 添加远程Node
     */
    public ScorpioRemoteNode addRemoteNode(String remoteId, String remoteAddr) {
        ScorpioRemoteNode remote = ScorpioRemoteNode.createActive(this, remoteId, remoteAddr);
        remoteNodes.put(remoteId, remote);
        log.info("[{}]建立主动远程Node连接: remote={}", id, remote);
        return remote;
    }
    
    /**
     * 添加被动远程Node(由远程连接进来)
     */
    public ScorpioRemoteNode addRemoteNodeInactive(String remoteId, String remoteAddr, Channel channel) {
        ScorpioRemoteNode remote = ScorpioRemoteNode.createInactive(this, remoteId, remoteAddr, channel);
        remoteNodes.put(remoteId, remote);
        log.info("[{}]建立被动远程Node连接: remote={}", id, remote);
        return remote;
    }
    
    /**
     * 删除远程Node
     */
    public void delRemoteNode(String remoteId) {
        ScorpioRemoteNode node = remoteNodes.remove(remoteId);
        if (node != null) {
            node.close();
        }
    }
    
    /**
     * 处理接收到的消息
     */
    public void handleMessage(RPCMessage msg, ScorpioRemoteNode remoteNode) {
        try {
            switch (msg.type) {
                case RPCMessage.TYPE_RPC:
                    handleRPCMessage(msg);
                    break;
                    
                case RPCMessage.TYPE_RPC_RETURN:
                    handleRPCReturnMessage(msg);
                    break;
                    
                default:
                    log.warn("未知的消息类型: msg={}", msg);
                    break;
            }
        } catch (Exception e) {
            log.error("处理消息异常: msg={}", msg, e);
        }
    }
    
    /**
     * 处理RPC调用消息
     */
    private void handleRPCMessage(RPCMessage msg) {
        ScorpioPort port = ports.get(msg.toPortId);
        if (port != null) {
            port.addCall(msg);
        } else {
            log.warn("处理RPC消息时未找到Port: msg={}", msg);
        }
    }
    
    /**
     * 处理RPC返回消息
     */
    private void handleRPCReturnMessage(RPCMessage msg) {
        ScorpioPort port = ports.get(msg.toPortId);
        if (port != null) {
            port.addCallResult(msg);
        } else {
            log.warn("处理RPC返回消息时未找到Port: msg={}", msg);
        }
    }
    
    /**
     * 发送消息到远程Node
     */
    public void sendRemoteMessage(RPCMessage msg) {
        String toNodeId = msg.toNodeId;
        
        if (id.equals(toNodeId)) {
            // 本Node直接处理
            handleLocalMessage(msg);
        } else {
            // 发送到远程Node
            ScorpioRemoteNode remote = remoteNodes.get(toNodeId);
            if (remote != null) {
                remote.sendMessage(msg);
            } else {
                log.error("发送消息时未找到远程Node: toNodeId={}", toNodeId);
            }
        }
    }
    
    /**
     * 处理本地消息
     */
    private void handleLocalMessage(RPCMessage msg) {
        ScorpioPort port = ports.get(msg.toPortId);
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
     * 获取Port
     */
    public ScorpioPort getPort(String portId) {
        return ports.get(portId);
    }
    
    /**
     * 添加Port
     */
    public void addPort(ScorpioPort port) {
        ports.put(port.getId(), port);
    }
    
    /**
     * 删除Port
     */
    public void delPort(ScorpioPort port) {
        ports.remove(port.getId());
    }
    
    /**
     * 获取所有远程Node
     */
    public Collection<ScorpioRemoteNode> getRemoteNodeAll() {
        return remoteNodes.values();
    }
    
    /**
     * 远程Node是否已连接
     */
    public boolean isRemoteNodeConnected(String remoteNodeName) {
        ScorpioRemoteNode node = remoteNodes.get(remoteNodeName);
        return node != null && node.isActive();
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getPartId() {
        return "".equals(partId) ? id : partId;
    }
    
    public String getAddr() {
        return addr;
    }
    
    public long getTimeCurrent() {
        return System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", getId())
                .append("addr", getAddr())
                .append("partId", getPartId())
                .toString();
    }
    
    /**
     * Scorpio Node Server Handler
     * 处理远程Node的连接和消息
     */
    private static class ScorpioNodeServerHandler extends SimpleChannelInboundHandler<RPCMessage> {
        private final ScorpioNode node;
        private String remoteNodeId;
        
        public ScorpioNodeServerHandler(ScorpioNode node) {
            this.node = node;
        }
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RPCMessage msg) throws Exception {
            switch (msg.type) {
                case RPCMessage.TYPE_PING:
                    // 第一次收到脉冲;建立被动连接
                    if (remoteNodeId == null && msg.methodParam != null && msg.methodParam.length >= 3) {
                        remoteNodeId = (String) msg.methodParam[0];
                        String remoteAddr = (String) msg.methodParam[1];
                        boolean main = (boolean) msg.methodParam[2];
                        if (main) {
                            // 只为主动连接建立反向连接
                            node.addRemoteNodeInactive(remoteNodeId, remoteAddr, ctx.channel());
                        }
                    }
                    
                    // 回复PONG
                    sendPong(ctx);
                    break;
                    
                case RPCMessage.TYPE_PONG:
                    // PONG已经在RemoteNode处理
                    break;
                    
                default:
                    // 其他消息转发给Node处理
                    ScorpioRemoteNode remote = node.remoteNodes.get(msg.fromNodeId);
                    if (remote != null) {
                        node.handleMessage(msg, remote);
                    }
                    break;
            }
        }
        
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state() == IdleState.READER_IDLE) {
                    // 读超时;发送脉冲
                    log.debug("连接空闲,发送脉冲: remote={}", remoteNodeId);
                }
            }
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (remoteNodeId != null) {
                log.warn("[{}]远程Node连接断开: remote={}", node.getId(), remoteNodeId);
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("[{}]远程Node连接异常: remote={}", node.getId(), remoteNodeId, cause);
            ctx.close();
        }
        
        private void sendPong(ChannelHandlerContext ctx) {
            RPCMessage pongMsg = RPCMessage.createPong(node.getId(), remoteNodeId);
            ctx.writeAndFlush(pongMsg);
        }
    }
}
