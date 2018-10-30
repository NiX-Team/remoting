# remoting-C/S模式下简洁可靠的通信框架
### 快速使用
1. 客户端
```java
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
```
2. 服务端
```java
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
        remotingService.start();
        System.in.read(new byte[1]);
    }
```
#### 数据包解析处理
对于数据包处理使用CQRS模式，这样使得用户对于数据处理具有高自定义性。
在定义数据处理的processor时我们需要给定processor处理的消息类型。RemotingCommand的code表明一个RemotingCommand数据包属于哪种消息类型，然后系统会根据你找到这个code码对应注册的processor进行数据处理。

### 可用，可靠性
模块完全在rocketmq的remoting模块提取。可用性，可靠性经过了rocketmq的检验。

## 基础结构图 时序图
### service管理模块
```plantuml
@startuml
interface RemotingService {
    start()
    shutdown()
    registerProcessor()
}
interface RemotingClient extends RemotingService{
    invokeSync()
    invokeAsync()
    invokeOneway()
}
interface RemotingServer extends RemotingService{
    invokeSync()
    invokeAsync()
    invokeOneway()
}
abstract NettyRemotingAbstract{
    semaphoreAsync
    processorTable
    responseTable
    processMessageReceived()//所有消息处理入口
    processRequestCommand()//处理请求消息
}

class NettyRemotingClient extends NettyRemotingAbstract implements RemotingClient{
    nettyClientConfig
    channelTables
    timer
    channelEventListener
    rpcHook
    
    class NettyClientHandler
    class NettyConnectManageHandler

}
class NettyRemotingServer extends NettyRemotingAbstract implements RemotingServer{
    nettyServerConfig
    timer
    rpcHook

    class HandshakeHandler
    class NettyServerHandler
    class NettyConnectManageHandler
}
@enduml
```

### 解码传输模块
```plantuml
@startuml

class RemotingCommand{
    requestId //command唯一id
    code //command类型
    body // command类容
    type //请求类型（req or resp）
}

Object byte
class NettyEncoder extends MessageToByteEncoder{
    encode()
}
class NettyDecoder extends LengthFieldBasedFrameDecoder{
    decode()
}
RemotingCommand --> NettyEncoder
NettyEncoder --> byte
byte --> NettyDecoder
NettyDecoder --> RemotingCommand
@end
```

### 处理模块
```plantuml
@startuml

interface NettyRequestProcessor {
    RemotingCommand processRequest(ctx, request)
    boolean rejectRequest();
}
@end
```
### 时序图
```plantuml
@startuml
== request ==
ref over Client, Command : code=1,type=request
Client -> Command : 生成命令
activate Command
Command -> invoke : 发送数据包
invoke -> CEncoder : 对command编码
participant CDecoder
rnote over invoke
 通过netty的
 pipeline处理到编码器
 endrnote
ref over CEncoder, ClientIO : byte[]
CEncoder -> ClientIO : 
ClientIO -> ServerIO :网络传输
activate ClientIO
ServerIO -> SDecoder : 解码器解码
rnote over ServerIO
 通过netty的
 pipeline处理到解码器
 endrnote
participant SEncoder
SDecoder -> ReqCommand : 解码得到command
ref over SDecoder, ReqCommand : code=1,type=request
== processor ==
ReqCommand -> ProcessorHandler :
ProcessorHandler -> Processor : 命令解析
rnote over ProcessorHandler
 ProcessorHandler根据
 command的code找到
 相应注册的processor
 endrnote
 Processor -> processor : 执行相应的处理逻辑
 processor -> respCommand : 处理结束
 collections processorRe
== response ==
respCommand -> SEncoder : 返回server处理结果
ref over SEncoder, respCommand : code=1,type=response
SEncoder -> ServerIO :
ref over SEncoder, ServerIO : byte[]
ServerIO -> ClientIO :
deactivate ClientIO
ClientIO -> CDecoder : 
CDecoder -> Command : 解析得到response
ref over CDecoder, Command : code=1,type=response
Command -> Client : end
deactivate Command
@enduml
```
