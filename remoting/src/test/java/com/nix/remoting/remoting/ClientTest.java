package com.nix.remoting.remoting;

import com.nix.remoting.remoting.exception.RemotingConnectException;
import com.nix.remoting.remoting.exception.RemotingSendRequestException;
import com.nix.remoting.remoting.exception.RemotingTimeoutException;
import com.nix.remoting.remoting.exception.RemotingTooMuchRequestException;
import com.nix.remoting.remoting.netty.*;
import com.nix.remoting.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author keray
 * @date 2018/10/16 下午9:35
 */
public class ClientTest {
    @Test
    public void client() throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, IOException, RemotingTooMuchRequestException {
        RemotingClient remotingClient = new NettyRemotingClient(new NettyClientConfig());
        //注册code=1消息的processor
        remotingClient.registerProcessor(1, new NettyRequestProcessor() {
            @Override
            public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
                return request;
            }

            @Override
            public boolean rejectRequest() {
                return false;
            }
        },new ThreadPoolExecutor(100,100,0, TimeUnit.MILLISECONDS,new LinkedBlockingDeque<>()));
        remotingClient.start();
        //发送一个消息code=1的消息
        RemotingCommand command = RemotingCommand.createRequestCommand(1,null);
        command.setBody("hello world".getBytes());
        //同步处理服务器响应
        System.out.println(new String(remotingClient.invokeSync("127.0.0.1:8888",command,1000).getBody()));
        command.setBody("hello world 1".getBytes());
        //异步处理服务器响应
        remotingClient.invokeAsync("127.0.0.1:8888", command, 1000, responseFuture -> System.out.println(new String(responseFuture.getResponseCommand().getBody())));
        System.in.read(new byte[1]);
    }
    @Test
    public void server() throws IOException {
        RemotingService remotingService = new NettyRemotingServer(new NettyServerConfig());
        remotingService.registerProcessor(1, new NettyRequestProcessor() {
            @Override
            public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
                //将request包改为response包
                request.markResponseType();
                System.out.println(request);
                ctx.writeAndFlush(request);
                return request;
            }

            @Override
            public boolean rejectRequest() {
                return false;
            }
        },new ThreadPoolExecutor(100,100,0,TimeUnit.MILLISECONDS,new LinkedBlockingDeque<>()));
        remotingService.registerRPCHook(new RPCHook() {
            @Override
            public void doBeforeRequest(String remoteAddr, RemotingCommand request) {

            }

            @Override
            public void doAfterResponse(String remoteAddr, RemotingCommand request, RemotingCommand response) {

            }
        });
        remotingService.start();
        System.in.read(new byte[1]);
    }
}
