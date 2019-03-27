/**
 * Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bftsmart.communication.client.netty;

import bftsmart.communication.client.CommunicationSystemServerSide;
import bftsmart.communication.client.RequestReceiver;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.util.Logger;
import bftsmart.tom.util.TOMUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * @author Paulo
 */
@Sharable public class NettyClientServerCommunicationSystemServerSide extends SimpleChannelInboundHandler<TOMMessage>
    implements CommunicationSystemServerSide {

    private static final org.slf4j.Logger log =
        LoggerFactory.getLogger(NettyClientServerCommunicationSystemServerSide.class);

    private RequestReceiver requestReceiver;
    private HashMap sessionTable;
    private ReentrantReadWriteLock rl;
    private ServerViewController controller;
    private boolean closed = false;
    private Channel mainChannel;

    // This locked seems to introduce a bottleneck and seems useless, but I cannot recall why I added it
    //private ReentrantLock sendLock = new ReentrantLock();
    private NettyServerPipelineFactory serverPipelineFactory;
    private org.slf4j.Logger logger = LoggerFactory.getLogger(NettyClientServerCommunicationSystemServerSide.class);

    public NettyClientServerCommunicationSystemServerSide(ServerViewController controller) {
        try {

            this.controller = controller;
            sessionTable = new HashMap();
            rl = new ReentrantReadWriteLock();

            //Configure the server.
            Mac macDummy = Mac.getInstance(controller.getStaticConf().getHmacAlgorithm());

            serverPipelineFactory =
                new NettyServerPipelineFactory(this, sessionTable, macDummy.getMacLength(), controller, rl,
                    TOMUtil.getSignatureSize(controller));

            EventLoopGroup bossGroup = new NioEventLoopGroup();

            //If the numbers of workers are not specified by the configuration file,
            //the event group is created with the default number of threads, which
            //should be twice the number of cores available.
            int nWorkers = this.controller.getStaticConf().getNumNettyWorkers();
            EventLoopGroup workerGroup = (nWorkers > 0 ? new NioEventLoopGroup(nWorkers) : new NioEventLoopGroup());

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(serverPipelineFactory.getDecoder());
                        ch.pipeline().addLast(serverPipelineFactory.getEncoder());
                        ch.pipeline().addLast(serverPipelineFactory.getHandler());
                    }
                }).childOption(ChannelOption.SO_KEEPALIVE, true).childOption(ChannelOption.TCP_NODELAY, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(
                new InetSocketAddress(controller.getStaticConf().getHost(controller.getStaticConf().getProcessId()),
                    controller.getStaticConf().getPort(controller.getStaticConf().getProcessId()))).sync();

            Logger.println("-- ID = " + controller.getStaticConf().getProcessId());
            Logger.println("-- N = " + controller.getCurrentViewN());
            Logger.println("-- F = " + controller.getCurrentViewF());
            Logger
                .println("-- Port = " + controller.getStaticConf().getPort(controller.getStaticConf().getProcessId()));
            Logger.println("-- requestTimeout = " + controller.getStaticConf().getRequestTimeout());
            Logger.println("-- maxBatch = " + controller.getStaticConf().getMaxBatchSize());
            if (controller.getStaticConf().getUseMACs() == 1)
                Logger.println("-- Using MACs");
            if (controller.getStaticConf().getUseSignatures() == 1)
                Logger.println("-- Using Signatures");
            //******* EDUARDO END **************//

            mainChannel = f.channel();

        } catch (NoSuchAlgorithmException ex) {
            Logger.printError(ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Logger.printError(ex.getMessage(), ex);
        }
    }

    private void closeChannelAndEventLoop(Channel c) {
        c.flush();
        c.deregister();
        c.close();
        c.eventLoop().shutdownGracefully();
    }

    @Override public void shutdown() {

        log.info("Shutting down Netty system");
        this.closed = true;

        closeChannelAndEventLoop(mainChannel);

        rl.readLock().lock();
        ArrayList<NettyClientServerSession> sessions = new ArrayList<>(sessionTable.values());
        rl.readLock().unlock();
        for (NettyClientServerSession ncss : sessions) {

            closeChannelAndEventLoop(ncss.getChannel());

        }

        java.util.logging.Logger.getLogger(NettyClientServerCommunicationSystemServerSide.class.getName())
            .log(Level.INFO, "NettyClientServerCommunicationSystemServerSide is halting.");

    }

    @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        if (this.closed) {
            closeChannelAndEventLoop(ctx.channel());
            return;
        }

        if (cause instanceof ClosedChannelException)
            log.info("Connection with client closed.");
        else if (cause instanceof ConnectException) {
            log.info("Impossible to connect to client.");
        } else {
            Logger.printError(cause.getMessage(), cause);
        }
    }

    @Override protected void channelRead0(ChannelHandlerContext ctx, TOMMessage sm) throws Exception {
        //TODO zyf modified
        log.info("---TOMMessage type: {}", sm.getReqType().toString());
        if (sm.getReqType() == TOMMessageType.RECONFIG) {
            log.info("ttptype" + sm.getSender());
        }
        if (this.closed) {
            closeChannelAndEventLoop(ctx.channel());
            return;
        }

        //delivers message to TOMLayer
        if (requestReceiver == null)
            Logger.println("RECEIVER NULO!!!!!!!!!!!!");
        else
            requestReceiver.requestReceived(sm);
    }

    @Override public void channelActive(ChannelHandlerContext ctx) {

        if (this.closed) {
            closeChannelAndEventLoop(ctx.channel());
            return;
        }
        log.info("netty client info : local address " + ctx.channel().localAddress() + " ;and remote address " + ctx
            .channel().remoteAddress());
    }

    @Override public void channelInactive(ChannelHandlerContext ctx) {

        if (this.closed) {
            closeChannelAndEventLoop(ctx.channel());
            return;
        }

        rl.writeLock().lock();
        try {
            Set s = sessionTable.entrySet();
            Iterator i = s.iterator();
            while (i.hasNext()) {
                Entry m = (Entry)i.next();
                NettyClientServerSession value = (NettyClientServerSession)m.getValue();
                if (ctx.channel().equals(value.getChannel())) {
                    int key = (Integer)m.getKey();
                    Logger.println("#Removing client channel with ID= " + key);
                    sessionTable.remove(key);
                    Logger.println("#active clients=" + sessionTable.size());
                    break;
                }
            }

        } finally {
            rl.writeLock().unlock();
        }
        log.info("Session Closed, active clients=" + sessionTable.size());
    }

    @Override public void setRequestReceiver(RequestReceiver tl) {
        this.requestReceiver = tl;
    }

    @Override public void send(int[] targets, TOMMessage sm, boolean serializeClassHeaders) {

        //serialize message
        DataOutputStream dos = null;

        byte[] data = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            dos = new DataOutputStream(baos);
            sm.wExternal(dos);
            dos.flush();
            data = baos.toByteArray();
            sm.serializedMessage = data;
        } catch (IOException ex) {
            Logger.println("Error enconding message.");
        } finally {
            try {
                dos.close();
            } catch (IOException ex) {
                Logger.println("Exception closing DataOutputStream: " + ex.getMessage());
            }
        }

        //replies are not signed in the current JBP version
        sm.signed = false;
        //produce signature if necessary (never in the current version)
        if (sm.signed) {
            //******* EDUARDO BEGIN **************//
            byte[] data2 = TOMUtil.signMessage(controller.getStaticConf().getRSAPrivateKey(), data);
            //******* EDUARDO END **************//
            sm.serializedMessageSignature = data2;
        }

        for (int i = 0; i < targets.length; i++) {
            rl.readLock().lock();
            //sendLock.lock();
            try {
                NettyClientServerSession ncss = (NettyClientServerSession)sessionTable.get(targets[i]);
                if (ncss != null) {
                    Channel session = ncss.getChannel();
                    sm.destination = targets[i];
                    //send message
                    session.writeAndFlush(
                        sm); // This used to invoke "await". Removed to avoid blockage and race condition.

                    ///////TODO: replace this patch for a proper client preamble
                } else if (sm.getSequence() >= 0 && sm.getSequence() <= 5) {

                    final int id = targets[i];
                    final TOMMessage msg = sm;

                    Thread t = new Thread() {

                        public void run() {

                            Logger.println("Received request from " + id
                                + " before establishing Netty connection. Re-trying until connection is established");

                            NettyClientServerSession ncss = null;
                            while (ncss == null) {

                                rl.readLock().lock();

                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException ex) {
                                    java.util.logging.Logger
                                        .getLogger(NettyClientServerCommunicationSystemServerSide.class.getName())
                                        .log(Level.SEVERE, null, ex);
                                }

                                ncss = (NettyClientServerSession)sessionTable.get(id);
                                if (ncss != null) {
                                    Channel session = ncss.getChannel();
                                    msg.destination = id;
                                    //send message
                                    session.writeAndFlush(msg);
                                }

                                rl.readLock().unlock();

                            }

                            Logger.println("Connection with " + id + " established!");

                        }

                    };

                    t.start();
                    ///////////////////////////////////////////
                } else {
                    Logger.println(
                        "!!!!!!!!NettyClientServerSession NULL !!!!!! sequence: " + sm.getSequence() + ", ID; "
                            + targets[i]);
                }
            } finally {
                //sendLock.unlock();
                rl.readLock().unlock();
            }
        }
    }

    @Override public int[] getClients() {

        rl.readLock().lock();
        Set s = sessionTable.keySet();
        int[] clients = new int[s.size()];
        Iterator it = s.iterator();
        int i = 0;
        while (it.hasNext()) {

            clients[i] = ((Integer)it.next()).intValue();
            i++;
        }

        rl.readLock().unlock();

        return clients;
    }

}