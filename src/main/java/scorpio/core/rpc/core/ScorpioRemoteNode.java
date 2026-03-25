package scorpio.core.rpc.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 远程节点连接
 * 使用Scorpio Client连接到远程Node
 */
public class ScorpioRemoteNode {
    private static final Logger log = LoggerFactory.getLogger(ScorpioRemoteNode.class);
    
    public static final long INTERVAL_PING = 6000;   // 脉冲间隔 6秒
    public static final long INTERVAL_LOST = 30000; // 连接丢失时间间隔 20秒
    
    private final String remoteId;               // 远程Node名称
    private final String remoteAddr;            // 远程Node地址
    private final ScorpioNode localNode;          // 本地Node
    
    private EventLoopGroup workerGroup;
    private Channel channel;
    
    private long createTime;                    // 连接创建时间
    private long lastPingTime = 0;              // 最后发送PING时间
    private long lastPongTime = 0;              // 最后收到PONG时间
    private boolean connected;                  // 是否已连接
    private boolean main;                        // 是否为主连接
    
    /**
     * 创建主动连接
     */
    public static ScorpioRemoteNode createActive(ScorpioNode localNode, String remoteName, String remoteAddr) {
        ScorpioRemoteNode remote = new ScorpioRemoteNode(localNode, remoteName, remoteAddr);
        remote.main = true;
        remote.connect();
        return remote;
    }
    
    /**
     * 创建被动连接
     */
    public static ScorpioRemoteNode createInactive(ScorpioNode localNode, String remoteName, String remoteAddr, Channel channel) {
        ScorpioRemoteNode remote = new ScorpioRemoteNode(localNode, remoteName, remoteAddr);
        remote.main = false;
        remote.channel = channel;
        remote.connected = true;
        remote.lastPongTime = System.currentTimeMillis();
        return remote;
    }
    
    /**
     * 构造函数
     */
    private ScorpioRemoteNode(ScorpioNode localNode, String remoteName, String remoteAddr) {
        this.localNode = localNode;
        this.remoteId = remoteName;
        this.remoteAddr = remoteAddr;
        this.createTime = System.currentTimeMillis();
        this.lastPongTime = createTime;
    }
    private String parseAddress(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.trim().isEmpty()) {
            throw new IllegalArgumentException("远程地址不能为空");
        }

        String addr = remoteAddr.trim();

        // 去掉协议前缀 (支持多种协议)
        int protocolEnd = addr.indexOf("://");
        if (protocolEnd != -1) {
            addr = addr.substring(protocolEnd + 3);
        } else if (addr.startsWith("tcp:") || addr.startsWith("ssl:")) {
            addr = addr.substring(4);
        }

        // 去掉开头的 /
        while (addr.startsWith("/")) {
            addr = addr.substring(1);
        }

        return addr;
    }
    /**
     * 建立连接
     */
    private void connect() {
        try {
            workerGroup = new NioEventLoopGroup();

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            pipeline.addLast(new IdleStateHandler(0, 0, INTERVAL_PING / 1000, TimeUnit.SECONDS));

                            pipeline.addLast(new RPCMessageCodec());

                            pipeline.addLast(new ScorpioRemoteNodeHandler(ScorpioRemoteNode.this));
                        }
                    });

            String addr = remoteAddr;
            if (addr.startsWith("tcp://")) {
                addr = addr.substring(6);
            } else if (addr.startsWith("tcp:")) {
                addr = addr.substring(4);
            }
            while (addr.startsWith("/")) {
                addr = addr.substring(1);
            }

            int lastColonIndex = addr.lastIndexOf(':');
            if (lastColonIndex == -1 || lastColonIndex == addr.length() - 1) {
                throw new IllegalArgumentException("地址格式错误：" + remoteAddr);
            }

            String host = addr.substring(0, lastColonIndex);
            String portStr = addr.substring(lastColonIndex + 1);
            int port = Integer.parseInt(portStr);

            ChannelFuture future = bootstrap.connect(host, port).sync();
            this.channel = future.channel();

            log.info("[{}]建立主动连接到远程 Node: remote={}, addr={}",
                    localNode.getId(), remoteId, remoteAddr);

        } catch (Exception e) {
            log.error("[{}]连接远程 Node 失败：remote={}, addr={}",
                    localNode.getId(), remoteId, remoteAddr, e);
            close();  // 关闭连接，清理资源
        }

    }
    
    /**
     * 发送RPC消息
     */
    public void sendMessage(RPCMessage msg) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (!future.isSuccess()) {
                        log.error("[{}]发送RPC消息失败: remote={}, msg={}", 
                                localNode.getId(), remoteId, msg, future.cause());
                        connected = false;
                    }
                }
            });
        } else {
            log.warn("[{}]远程节点未连接;消息发送失败: remote={}", localNode.getId(), remoteId);
        }
    }
    
    /**
     * 发送脉冲检测
     */
    public void sendPing() {
        RPCMessage pingMsg = RPCMessage.createPing(
                localNode.getId(), 
                remoteId,
                localNode.getAddr(),
                main,
                new Object[] { localNode.getId(), localNode.getAddr(), main }
        );
        sendMessage(pingMsg);
    }
    
    /**
     * 发送脉冲响应
     */
    public void sendPong() {
        RPCMessage pongMsg = RPCMessage.createPong(localNode.getId(), remoteId);
        sendMessage(pongMsg);
    }
    
    /**
     * 收到脉冲检测
     */
    public void onPingReceived() {
        sendPong();
    }
    
    /**
     * 收到脉冲响应
     */
    public void onPongReceived() {
        if (!connected) {
            log.info("[{}]激活远程Node连接: remote={}", localNode.getId(), remoteId);
        } else {
            log.debug("[{}]收到远程Node连接脉冲: remote={}", localNode.getId(), remoteId);
        }
        connected = true;
        lastPongTime = System.currentTimeMillis();
    }
    
    /**
     * 脉冲检测
     */
    public void pulse(long now) {

        if (now - lastPingTime >= INTERVAL_PING) {
            sendPing();//定时发送PING消息
            lastPingTime = now;
        }
        // 检查连接是否丢包
        if (connected && (now - lastPongTime) > INTERVAL_LOST) {
            connected = false;
            log.error("[{}]失去远程Node连接: remote={}", localNode.getId(), remoteId);
            
            // 主动连接需要重连
            if (main) {
                log.info("[{}]尝试重连远程Node: remote={}", localNode.getId(), remoteId);
                reconnect();
            }
        }
    }
    
    /**
     * 重新连接
     */
    private void reconnect() {
        close();
        connect();
    }
    
    /**
     * 关闭连接
     */
    public void close() {
        connected = false;
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
    }
    
    // Getters
    public String getRemoteId() {
        return remoteId;
    }
    
    public String getRemoteAddr() {
        return remoteAddr;
    }
    
    public boolean isActive() {
        return connected && channel != null && channel.isActive();
    }
    
    public boolean isMain() {
        return main;
    }
    
    public String getLocalId() {
        return localNode.getId();
    }
    
    public long getPongTime() {
        return lastPongTime;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("remoteId", remoteId)
                .append("remoteAddr", remoteAddr)
                .append("connected", connected)
                .append("main", main)
                .append("createTime", createTime)
                .append("pingTime", lastPingTime)
                .append("pongTime", lastPongTime)
                .toString();
    }
    
    /**
     * 远程节点消息处理器
     */
    private static class ScorpioRemoteNodeHandler extends SimpleChannelInboundHandler<RPCMessage> {
        private final ScorpioRemoteNode remoteNode;
        
        public ScorpioRemoteNodeHandler(ScorpioRemoteNode remoteNode) {
            this.remoteNode = remoteNode;
        }
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RPCMessage msg) throws Exception {
            // 处理收到的消息
            switch (msg.type) {
                case RPCMessage.TYPE_PING:
                    remoteNode.onPingReceived();
                    break;
                    
                case RPCMessage.TYPE_PONG:
                    remoteNode.onPongReceived();
                    break;
                    
                default:
                    // 其他消息类型转发给Node处理
                    remoteNode.localNode.handleMessage(msg, remoteNode);
                    break;
            }
        }
        
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state() == IdleState.ALL_IDLE) {
                    // 读写皆空时发送脉冲
                    remoteNode.sendPing();
                }
            }
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            remoteNode.connected = false;
            log.warn("[{}]远程Node连接断开: remote={}", 
                    remoteNode.localNode.getId(), remoteNode.remoteId);
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("[{}]远程Node连接异常: remote={}", 
                    remoteNode.localNode.getId(), remoteNode.remoteId, cause);
            ctx.close();
        }
    }
}
