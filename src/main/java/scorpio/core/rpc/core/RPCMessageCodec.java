package scorpio.core.rpc.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * RPC消息编解码器
 * 使用JSON序列化;兼容Scorpio框架的Param格式
 */
public class RPCMessageCodec extends MessageToMessageCodec<ByteBuf, RPCMessage> {
    private static final Logger log = LoggerFactory.getLogger(RPCMessageCodec.class);
    
    @Override
    protected void encode(ChannelHandlerContext ctx, RPCMessage msg, List<Object> out) throws Exception {
        try {
            // 使用JSON序列化消息
            String json = JSON.toJSONString(msg, SerializerFeature.WriteDateUseDateFormat);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            
            // 创建ByteBuf: 4字节长度 + 数据内容
            ByteBuf buffer = ctx.alloc().buffer(4 + bytes.length);
            buffer.writeInt(bytes.length);
            buffer.writeBytes(bytes);
            
            out.add(buffer);
        } catch (Exception e) {
            log.error("编码RPC消息失败: msg={}", msg, e);
            throw e;
        }
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        try {
            // 检查是否有足够的数据读取长度
            if (msg.readableBytes() < 4) {
                return;
            }
            
            // 标记读指针
            msg.markReaderIndex();
            
            // 读取数据长度
            int length = msg.readInt();
            
            // 检查是否有足够的数据读取内容
            if (msg.readableBytes() < length) {
                // 重置读指针;等待更多数据
                msg.resetReaderIndex();
                return;
            }
            
            // 读取JSON字符串
            byte[] bytes = new byte[length];
            msg.readBytes(bytes);
            String json = new String(bytes, StandardCharsets.UTF_8);
            
            // 反序列化为RPCMessage
            RPCMessage rpcMessage = JSON.parseObject(json, RPCMessage.class);
            out.add(rpcMessage);
            
        } catch (Exception e) {
            log.error("解码RPC消息失败", e);
            throw e;
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("RPC消息编解码异常", cause);
        super.exceptionCaught(ctx, cause);
    }
}
