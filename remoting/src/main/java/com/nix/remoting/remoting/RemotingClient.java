/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nix.remoting.remoting;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.nix.remoting.remoting.exception.RemotingConnectException;
import com.nix.remoting.remoting.exception.RemotingSendRequestException;
import com.nix.remoting.remoting.exception.RemotingTimeoutException;
import com.nix.remoting.remoting.exception.RemotingTooMuchRequestException;
import com.nix.remoting.remoting.protocol.RemotingCommand;
import com.nix.remoting.remoting.netty.NettyRequestProcessor;

/**
 * @author jingxun.zds
 * 远程连接客户端
 */
public interface RemotingClient extends RemotingService {

    /**
     * 同步等待server响应
     * */
    RemotingCommand invokeSync(final String addr, final RemotingCommand request,
                               final long timeoutMillis) throws InterruptedException, RemotingConnectException,
            RemotingSendRequestException, RemotingTimeoutException;

    /**
     * 异步等待server响应
     * */
    void invokeAsync(final String addr, final RemotingCommand request, final long timeoutMillis,
        final InvokeCallback invokeCallback) throws InterruptedException, RemotingConnectException,
            RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    /**
     * 设置异步{@link com.nix.remoting.remoting.netty.ResponseFuture} 执行器
     * */
    void setCallbackExecutor(final ExecutorService callbackExecutor);
}
