package happy2b.woody.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.PrintWriter;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/20
 */
public class WoodyClient {

    // 服务器地址和端口
    private final String host;
    private final int port;
    private final long pid;

    private String terminalCommandFlag;

    public WoodyClient(String host, int port, long pid) {
        this.host = host;
        this.port = port;
        this.pid = pid;
        this.terminalCommandFlag = String.format("[woody@%d]$ ", pid);
    }

    public void boot() throws Exception {
        Terminal terminal = null;
        EventLoopGroup group = null;
        Channel channel = null;
        try {
            terminal = TerminalBuilder.builder().system(true).jansi(true).build();
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            PrintWriter terminalWriter = terminal.writer();

            ChannelInboundHandlerAdapter channelHandler = new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    terminalWriter.println(msg);
                    terminalWriter.flush();
                    terminalWriter.print(terminalCommandFlag);
                    terminalWriter.flush();
                }
            };
            group = new NioEventLoopGroup();
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(channelHandler);
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            System.out.println("已连接到服务器: " + host + ":" + port);

            channel = future.channel();

            System.out.println("请输入内,输入stop退出:");
            String input;
            while ((input = safeReadLine(reader)) != null) {
                if (input.isEmpty()) {
                    continue;
                }
                if ("stop".equalsIgnoreCase(input.trim())) {
                    channel.writeAndFlush("stop");
                    System.out.println("程序退出");
                    break;
                }
                channel.writeAndFlush(input);
            }
            // 等待通道关闭
            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (terminal != null) {
                terminal.close();
            }
            if (group != null) {
                group.shutdownGracefully();
            }
        }
    }

    private String safeReadLine(LineReader reader) {
        try {
            return reader.readLine(terminalCommandFlag);
        } catch (Exception e) {
            return "";
        }
    }

}
