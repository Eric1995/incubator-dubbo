package org.apache.dubbo.flux;

import static org.apache.dubbo.common.Constants.*;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;

import com.ztesoft.mq.client.api.model.ProduceResult;
import org.apache.commons.io.FileUtils;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ztesoft.mq.client.api.MQClientFactory;
import com.ztesoft.mq.client.api.common.PropertyKey;
import com.ztesoft.mq.client.api.model.MQMessage;
import com.ztesoft.mq.client.api.producer.Producer;
import com.ztesoft.mq.client.impl.MQClientFactoryImpl;

public class FluxStatZcmExporter implements FluxStatExporter {

    private static final Logger logger = LoggerFactory.getLogger(FluxStatZcmExporter.class);

    private SimpleDateFormat sdf;

    private Producer producer;

    /**
     * 是否初始化成功
     */
    private boolean initialized = false;

    private String containerId;

    private String cloudAppId;

    public FluxStatZcmExporter() {
        try {
            FluxStatCollector fluxStatCollector = FluxStatCollector.getCollector(null);
            if (StringUtils.isEmpty(fluxStatCollector.getNameServerAddr())) {
                throw new IllegalArgumentException("Name server address is not configured");
            }
            Properties props = new Properties();
            props.put(PropertyKey.Namesrv_Addr, fluxStatCollector.getNameServerAddr());
            props.put(PropertyKey.Producer_Id, fluxStatCollector.getProducerId());
            props.put("ConsumerMessageModel", "CLUSTERING");
            MQClientFactory factory = new MQClientFactoryImpl();
            this.producer = factory.createProducer(props);
            this.producer.start();

            this.sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            this.sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            this.cloudAppId = getCloudAppId();
            this.containerId = getContainerId();

            this.initialized = true;
        } catch (Throwable t) {
            logger.error("Failed to init " + FluxStatZcmExporter.class.getSimpleName() + ", cause: " + t.getMessage(), t);
        }
    }

    public void export(Map<FluxStatKey, long[]> data, long statTime) {
        if (initialized) {
            FluxStatCollector fluxStatCollector = FluxStatCollector.getCollector(null);
            String topic = fluxStatCollector.getTopic();
            long interval = fluxStatCollector.getInterval();
            String statTimeStr = sdf.format(new Date(statTime * interval));

            for (Entry<FluxStatKey, long[]> e : data.entrySet()) {
                FluxStatKey fluxStatKey = e.getKey();
                long[] stats = e.getValue();
                String[] keys = new String[]{ //
                        SIDE_KEY,
                        SERVICE_INTERFACE_KEY,
                        SERVICE_VERSION_KEY,
                        SERVICE_GROUP_KEY,
                        FLUX_STAT_SUCCESS_TIMES_KEY, //
                        FLUX_STAT_FAILURE_TIMES_KEY, //
                        FLUX_STAT_TIMEOUT_TIMES_KEY, //
                        FLUX_STAT_REJECTION_TIMES_KEY, //
                        FLUX_STAT_NOPROVIDER_TIMES_KEY,
                        FLUX_STAT_OVERLOAD_TIMES_KEY, //
                        FLUX_STAT_TOTAL_COST_KEY, //
                        FLUX_STAT_MAX_COST_KEY, //
                        FLUX_STAT_AVG_COST_KEY,
                        FLUX_STAT_ZCM_TPS_KEY,
                        FLUX_STAT_ZCM_SUCCESS_RATE_KEY,
                        FLUX_STAT_ZCM_INDEX_NAME_KEY
                };
                Object[] values = new Object[]{ //
                        fluxStatKey.getSide(),
                        fluxStatKey.getService(),
                        StringUtils.isNotEmpty(fluxStatKey.getVersion()) ? fluxStatKey.getVersion() : "",
                        StringUtils.isNotEmpty(fluxStatKey.getGroup()) ? fluxStatKey.getGroup() : "",
                        stats[0], //
                        stats[1], //
                        stats[2], //
                        stats[10], //
                        stats[9],
                        stats[8], //
                        stats[3], //
                        stats[4], //
                        Float.parseFloat(new DecimalFormat("##0.00").format(stats[0] + stats[1] > 0 ? (float) stats[3] / (stats[0] + stats[1]) : 0.00)),
                        Float.parseFloat(new DecimalFormat("##0.00").format(stats[0] + stats[1] > 0 ? (float) (stats[0] + stats[1]) / (interval / 1000) : 0.00)),
                        Float.parseFloat(new DecimalFormat("##0.00").format(stats[0] + stats[1] > 0 ? 100 * ((float) stats[0]) / (stats[0] + stats[1]) : 0.00)),
                        FLUX_STAT_ZCM_DEFAULT_INDEX_NAME
                };
                JSONArray jsonArray = new JSONArray();
                addToJsonArray(jsonArray, keys, values, statTimeStr);
                JSONObject result = new JSONObject();
                result.put(FLUX_STAT_ZCM_EVENT_TYPE_KEY, FLUX_STAT_ZCM_DEFAULT_EVENT_TYPE);
                result.put(FLUX_STAT_ZCM_EVENT_TIME_KEY, statTimeStr);
                result.put(FLUX_STAT_ZCM_CLOUD_APP_ID_KEY, cloudAppId);
                result.put(FLUX_STAT_ZCM_CLOUD_NODE_ID_KEY, containerId);
                result.put(FLUX_STAT_VERSION_KEY, 2);
                result.put(FLUX_STAT_ZCM_EVENT_KEY, jsonArray);

                try {
                    System.err.println(result.toJSONString());
                    byte[] body = result.toString().getBytes("UTF-8");
                    MQMessage message = new MQMessage(topic, null, body);
                    producer.send(message);
                } catch (Throwable t) {
                    logger.error("Failed to send flux stats to ZCM", t);
                }
            }
        }
    }

    private void addToJsonArray(JSONArray target, String[] keys, Object[] values, String time) {
        JSONObject statItemJson = new JSONObject();
        for (int i = 0; i < keys.length; i++) {
            statItemJson.put(keys[i], values[i]);
        }
        target.add(statItemJson);
    }

    private String getCloudAppId() {
        String cloudAppId = System.getenv(Constants.CLOUD_APP_ID_ENV_VAR_KEY);
        if (cloudAppId == null) {
            cloudAppId = FluxStatCollector.getCollector(null).getApplication();
        }
        return cloudAppId;
    }

    private String getContainerId() {
        String containerId = null;
        String path = "/proc/1/cpuset";
        File file = new File(path);
        if (file.exists()) {
            try {
                /* /docker/e5b0cf0b9180780305f5aab4e886c14d1d676d6b9468a70f1399827f8dc3dc76 */
                String fileContent = FileUtils.readFileToString(file);
                if (StringUtils.isNotEmpty(fileContent)) {
                    String[] fileContents = fileContent.split("/");
                    if (fileContents[fileContents.length - 1].length() >= 12) {
                        containerId = fileContents[fileContents.length - 1];
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }
        if (StringUtils.isEmpty(containerId)) {
            containerId = getCloudAppId() + ConfigUtils.getPid();
        }
        return containerId;
    }

}
