package happy2b.woody.core.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/19
 */
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connect: " + ctx.channel().remoteAddress());
    }

    // 当收到客户端消息时调用
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String message = (String) msg;
        System.out.println("accept client message: " + message);

        ctx.writeAndFlush(System.currentTimeMillis() + "\r\n" + "asdfasdfasda");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
