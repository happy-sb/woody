package happy2b.woody.core.server;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.common.bytecode.InstrumentationUtils;
import happy2b.woody.common.thread.AgentThreadFactory;
import happy2b.woody.core.config.Configure;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/18
 */
public class WoodyBootstrap {


    private static final Logger logger = LoggerFactory.getLogger(WoodyBootstrap.class);

    private static WoodyBootstrap bootstrapInstance;


    private File outputPath;
    private Configure configure;

    private int pid;

    private AtomicBoolean isBindRef = new AtomicBoolean(false);

    private Thread shutdown;
    private ExecutorService executorService;

    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private CountDownLatch latch = new CountDownLatch(1);


    private WoodyBootstrap(Configure configure, Instrumentation instrumentation) throws Throwable {
        this.configure = configure;
        this.pid = configure.getJavaPid();

        InstrumentationUtils.setInstrumentation(instrumentation);

        executorService = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                final Thread t = new Thread(r, "as-command-execute-daemon");
                t.setDaemon(true);
                return t;
            }
        });

        shutdown = AgentThreadFactory.newAgentThread(AgentThreadFactory.AgentThread.SHUTDOWN_HOOK, new Runnable() {
            @Override
            public void run() {
                WoodyBootstrap.this.destroy();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    WoodyBootstrap.this.bind();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        latch.await();

        Runtime.getRuntime().addShutdownHook(shutdown);
    }

    public static WoodyBootstrap getInstance() {
        return bootstrapInstance;
    }

    public static WoodyBootstrap getInstance(Instrumentation instrumentation, String args) throws Throwable {
        if (bootstrapInstance != null) {
            return bootstrapInstance;
        }
        bootstrapInstance = new WoodyBootstrap(Configure.toConfigure(args), instrumentation);
        return bootstrapInstance;
    }

    public void bind() throws Throwable {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        try {
            // 服务器启动辅助类
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    // 设置通道类型为NIO
                    .channel(NioServerSocketChannel.class)
                    // 设置服务器端口
                    .localAddress(configure.getServerPort())
                    // 配置通道选项
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 配置子通道处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new StringEncoder());
                            ch.pipeline().addLast(new WoodyServerHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind().sync();
            serverChannel = future.channel();
            isBindRef.set(true);
            latch.countDown();
            serverChannel.closeFuture().sync();
        } catch (Throwable e) {
            latch.countDown();
            isBindRef.set(false);
            throw e;
        } finally {
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
        }
    }


    public boolean isBind() {
        return isBindRef.get();
    }

    public void destroy() {
        serverChannel.writeAndFlush("stop");
        executorService.shutdownNow();
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        bootstrapInstance = null;
    }

    public Channel getServerChannel() {
        return serverChannel;
    }

    public void writeCommand(WoodyCommand command) {
        serverChannel.writeAndFlush(WoodyServerHandler.JSON_ADAPTER.toJson(command));
    }

    public String getWoodyHome() {
        return configure.getWoodyHomeDir();
    }
}
