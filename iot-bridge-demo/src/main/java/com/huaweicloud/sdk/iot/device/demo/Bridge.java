/*
 * Copyright (c) 2020-2023 Huawei Cloud Computing Technology Co., Ltd. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without specific prior written
 *    permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.huaweicloud.sdk.iot.device.demo;

import com.huaweicloud.sdk.iot.device.IoTDevice;
import com.huaweicloud.sdk.iot.device.client.requests.CommandRsp;

import io.netty.channel.Channel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 此例子用来演示如何使用协议网桥来实现TCP协议设备接入。网桥为每个TCP设备创建一个客户端（IotClient），使用设备的身份
 * 和平台进行通讯。本例子TCP server传输简单的字符串，并且首条消息会发送设备标识来鉴权。用户可以自行扩展StringTcpServer类
 * 来实现更复杂的TCP server。
 */
public class Bridge {

    private static final Logger log = LogManager.getLogger(Bridge.class);

    private static Bridge instance;

    private final DeviceIdentityRegistry deviceIdentityRegistry;

    private final String serverUri;

    private final Map<String, Session> deviceIdToSessionMap;

    private final Map<String, Session> channelIdToSessionMap;

    private Bridge(String serverUri, DeviceIdentityRegistry deviceIdentityRegistry) {
        this.serverUri = serverUri;

        if (deviceIdentityRegistry == null) {
            deviceIdentityRegistry = new DefaultDeviceIdentityRegistry();
        }
        this.deviceIdentityRegistry = deviceIdentityRegistry;
        deviceIdToSessionMap = new ConcurrentHashMap<>();
        channelIdToSessionMap = new ConcurrentHashMap<>();
    }

    static Bridge getInstance() {
        return instance;
    }

    private static void createBridge(String serverUri, DeviceIdentityRegistry deviceIdentityRegistry) {
        instance = new Bridge(serverUri, deviceIdentityRegistry);
    }

    public static void main(String[] args) throws Exception {

        // 用户请替换为自己的接入地址。
        String demoServerUri = "ssl://xxx.st1.iotda-device.cn-north-4.myhuaweicloud.com:8883";

        int port = 8080;

        Bridge.createBridge(demoServerUri, null);

        new TcpServer(port).run();

    }

    Session getSessionByChannel(String channelId) {
        return channelIdToSessionMap.get(channelId);
    }

    void removeSession(String channelId) {
        Session session = channelIdToSessionMap.get(channelId);
        if (session != null) {
            session.getDeviceClient().close();
            deviceIdToSessionMap.remove(session.getDeviceId());
        }
        channelIdToSessionMap.remove(channelId);

    }

    void createSession(String nodeId, Channel channel) {

        // 根据设备识别码获取设备标识信息
        DeviceIdentity deviceIdentity = deviceIdentityRegistry.getDeviceIdentity(nodeId);
        if (deviceIdentity == null) {
            log.warn("deviceIdentity is null");
            return;
        }

        // 加载iot平台的ca证书，进行服务端校验
        URL resource = Bridge.class.getClassLoader().getResource("ca.jks");
        File file = new File(resource.getPath());

        String deviceId = deviceIdentity.getDeviceId();
        IoTDevice ioTDevice = new IoTDevice(serverUri, deviceId, deviceIdentity.getSecret(), file);
        int ret = ioTDevice.init();
        if (ret != 0) {
            return;
        }

        // 创建会话
        Session session = new Session();
        session.setChannel(channel);
        session.setNodeId(nodeId);
        session.setDeviceId(deviceId);
        session.setDeviceClient(ioTDevice.getClient());

        // 设置下行回调
        ioTDevice.getClient().setDeviceMessageListener(deviceMessage -> {

            // 这里可以根据需要进行消息格式转换
            channel.writeAndFlush(deviceMessage.getContent());
        });

        ioTDevice.getClient().setCommandListener((requestId, serviceId, commandName, paras) -> {

            // 这里可以根据需要进行消息格式转换
            channel.writeAndFlush(paras);

            // 为了简化处理，我们在这里直接回命令响应。更合理做法是在设备处理完后再回响应
            ioTDevice.getClient().respondCommand(requestId, new CommandRsp(0));
        });

        ioTDevice.getClient().setPropertyListener(new DefaultBridgePropertyListener(channel, ioTDevice));

        // 保存会话
        deviceIdToSessionMap.put(deviceId, session);
        channelIdToSessionMap.put(channel.id().asLongText(), session);

        log.info("create new session");

    }

}
