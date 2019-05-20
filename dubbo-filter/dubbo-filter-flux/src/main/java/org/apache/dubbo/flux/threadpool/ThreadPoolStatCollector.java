package org.apache.dubbo.flux.threadpool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stat.StatCollector;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.RpcInvocation;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class ThreadPoolStatCollector implements StatCollector {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolStatCollector.class);

    private long interval;

    private final ConcurrentHashMap<String, LoadingCache<Long, AtomicLong>> counters = new ConcurrentHashMap<String, LoadingCache<Long, AtomicLong>>();

    public void collect(Object obj) {
        if (isInitialized() && obj instanceof RpcInvocation) {
            RpcInvocation rpcInvocation = (RpcInvocation) obj;
            URL tempUrl = new URL(Constants.THREADPOOL_KEY, NetUtils.getLocalHost(), 0, rpcInvocation.getAttachments().get(Constants.INTERFACE_KEY), //
                    Constants.INTERFACE_KEY, rpcInvocation.getAttachments().get(Constants.INTERFACE_KEY), //
                    Constants.VERSION_KEY, "0.0.0".equals(rpcInvocation.getAttachments().get(Constants.VERSION_KEY)) ? null : rpcInvocation.getAttachments().get(Constants.VERSION_KEY), //
                    Constants.GROUP_KEY, rpcInvocation.getAttachments().get(Constants.GROUP_KEY));
            String serviceKey = tempUrl.getServiceKey();
            if (StringUtils.isEmpty(serviceKey)) {
                return;
            }
            LoadingCache<Long, AtomicLong> counter = counters.get(serviceKey);
            if (counter == null) {
                counter = CacheBuilder.newBuilder().expireAfterWrite(interval * 3, TimeUnit.MILLISECONDS).build(new CacheLoader<Long, AtomicLong>() {
                    public AtomicLong load(Long key) throws Exception {
                        return new AtomicLong();
                    }
                });
                LoadingCache<Long, AtomicLong> existingCounter = counters.putIfAbsent(serviceKey, counter);
                if (existingCounter != null) {
                    counter.cleanUp();
                    counter = existingCounter;
                }
            }
            try {
                counter.get(System.currentTimeMillis() / interval).incrementAndGet();
            } catch (ExecutionException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public Object get(Map<String, String> param) {
        if (isInitialized()) {
            String serviceKey = param.get(Constants.FLUX_STAT_SERVICE_KEY);
            String statTime = param.get(Constants.FLUX_STAT_TIME_KEY);
            LoadingCache<Long, AtomicLong> counter = counters.get(serviceKey);
            if (counter != null) {
                try {
                    return counter.get(Long.valueOf(statTime)).get();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return 0l;
    }

    public void init(Map<String, String> param) {
        String interval = param.get(Constants.FLUX_STAT_INTERVAL_KEY);
        this.interval = Long.valueOf(interval);
    }

    private boolean isInitialized() {
        return interval > 0;
    }

}
