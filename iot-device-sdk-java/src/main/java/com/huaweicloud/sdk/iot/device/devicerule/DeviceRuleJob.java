package com.huaweicloud.sdk.iot.device.devicerule;

import com.huaweicloud.sdk.iot.device.devicerule.model.DeviceRuleAction;
import com.huaweicloud.sdk.iot.device.devicerule.model.TimeRange;
import com.huaweicloud.sdk.iot.device.utils.ExceptionUtil;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.List;

@Slf4j
public class DeviceRuleJob implements Job {

    @Override
    public void execute(JobExecutionContext context) {
        try {
            final List<DeviceRuleAction> actionList = (List<DeviceRuleAction>) context.getMergedJobDataMap()
                .get("actionList");
            final DeviceRuleService deviceRuleService = (DeviceRuleService) context.getMergedJobDataMap()
                .get("deviceRuleService");
            final TimeRange timeRange = (TimeRange) context.getMergedJobDataMap().get("timeRange");
            if (deviceRuleService.checkTimeRange(timeRange)) {
                deviceRuleService.onRuleActionHandler(actionList);
            }
        } catch (Exception e) {
            log.warn("failed to execute DeviceRuleJob, exception={}", ExceptionUtil.getBriefStackTrace(e));
        }
    }
}
