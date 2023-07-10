package com.huaweicloud.sdk.iot.device.demo;

import com.huaweicloud.sdk.iot.device.IoTDevice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * 设备信息样例，建议在设备建链成功后调用。
 */
public class ReportDeviceInfoSample {

    private static final String IOT_ROOT_CA_RES_PATH = "ca.jks";

    private static final String IOT_ROOT_CA_TMP_PATH = "huaweicloud-iotda-tmp-" + IOT_ROOT_CA_RES_PATH;

    public static void main(String[] args) throws IOException {
        String serverUri = "ssl://iot-mqtts.cn-north-4.myhuaweicloud.com:8883";
        String deviceId = "702b1038-a174-4a1d-969f-f67f8df43c4a";
        String secret = "mysecret";

        // 加载iot平台的ca证书，进行服务端校验
        File tmpCAFile = new File(IOT_ROOT_CA_TMP_PATH);
        try (InputStream resource = CommandSample.class.getClassLoader().getResourceAsStream(IOT_ROOT_CA_RES_PATH)) {
            Files.copy(resource, tmpCAFile.toPath(), REPLACE_EXISTING);
        }

        // 创建设备
        IoTDevice device = new IoTDevice(serverUri, deviceId, secret, tmpCAFile);

        if (device.init() != 0) {
            return;
        }

        String swVersion = "v1.0";
        String fwVersion = "v1.0";
        device.getClient().reportDeviceInfo(swVersion, fwVersion, null);
    }
}