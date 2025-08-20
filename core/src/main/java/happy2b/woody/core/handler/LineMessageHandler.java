package happy2b.woody.core.handler;

import happy2b.woody.util.common.PidUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class LineMessageHandler extends ChannelInboundHandlerAdapter {

    private String cmd = String.format("[woody@%d]$ ", PidUtils.currentLongPid());

    // 接收拆分后的完整消息（已去除换行符）
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String message = (String) msg;
        System.out.println("收到完整消息: " + message);

        // 回复客户端（自动添加换行符，便于客户端拆分）
        ctx.writeAndFlush(cmd);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}

