package com.huaweicloud.bridge.sdk.handler;

import com.huaweicloud.bridge.sdk.BridgeClient;
import com.huaweicloud.bridge.sdk.constants.BridgeSDKConstants;
import com.huaweicloud.sdk.iot.device.client.handler.MessageReceivedHandler;
import com.huaweicloud.sdk.iot.device.transport.RawMessage;
import com.huaweicloud.sdk.iot.device.utils.IotUtil;
import com.huaweicloud.sdk.iot.device.utils.JsonUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DeviceLoginHandler implements MessageReceivedHandler {
    private static final Logger log = LogManager.getLogger(DeviceLoginHandler.class);

    private final BridgeClient bridgeClient;

    public DeviceLoginHandler(BridgeClient bridgeClient) {
        this.bridgeClient = bridgeClient;
    }

    @Override
    public void messageHandler(RawMessage message) {
        log.debug("received the response of the device under one bridge logins, the  message is {}",
            message);
        String requestId = IotUtil.getRequestId(message.getTopic());
        String deviceId = IotUtil.getDeviceId(message.getTopic());
        Map map = JsonUtil.convertJsonStringToObject(message.toString(), Map.class);
        if (map == null) {
            log.warn("the response of device login is invalid. ");
            return;
        }
        int resultCode = (int) map.get(BridgeSDKConstants.RESULET_CODE);

        if (bridgeClient.getLoginListener() != null) {
            bridgeClient.getLoginListener().onLogin(deviceId, requestId, resultCode);
            return;
        }

        CompletableFuture<Integer> future = bridgeClient.getRequestIdCache().getFuture(requestId);
        Optional.ofNullable(future)
            .ifPresent(integerCompletableFuture -> integerCompletableFuture.complete(resultCode));
    }
}