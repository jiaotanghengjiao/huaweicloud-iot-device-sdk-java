package com.huaweicloud.sdk.iot.device.client.handler;

import com.huaweicloud.sdk.iot.device.client.DeviceClient;
import com.huaweicloud.sdk.iot.device.client.requests.Shadow;
import com.huaweicloud.sdk.iot.device.transport.RawMessage;
import com.huaweicloud.sdk.iot.device.utils.IotUtil;
import com.huaweicloud.sdk.iot.device.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShadowHandler implements MessageReceivedHandler {

    private final DeviceClient deviceClient;

    public ShadowHandler(DeviceClient deviceClient) {
        this.deviceClient = deviceClient;
    }

    @Override
    public void messageHandler(RawMessage message) {
        String topic = message.getTopic();
        String requestId = IotUtil.getRequestId(topic);

        final Shadow shadow = JsonUtil.convertJsonStringToObject(message.toString(), Shadow.class);
        if (shadow == null) {
            log.warn("invalid shadow");
            return;
        }

        if (deviceClient.getShadowListener() != null && (shadow.getDeviceId() == null || shadow.getDeviceId()
            .equals(deviceClient.getDeviceId()))) {
            deviceClient.getShadowListener().onShadow(requestId, shadow.getShadow());
            return;
        }

        deviceClient.getDevice().onShadow(requestId, shadow);
    }
}
