package org.apache.dubbo.flux;

import static org.apache.dubbo.flux.FluxStatKey.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stat.StatCollector;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class FluxStatCollector {

	private static Holder<FluxStatCollector> collectorHolder = new Holder<FluxStatCollector>();

	private static final Logger logger = LoggerFactory.getLogger(FluxStatCollector.class);

	public static final int LENGTH = 10;

	public static final ConcurrentHashMap<FluxStatKey, LoadingCache<Long, AtomicReference<long[]>>> statisticsMap = new ConcurrentHashMap<FluxStatKey, LoadingCache<Long, AtomicReference<long[]>>>();

	private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("DubboFluxStatSendTimer", true));

	public static long interval;

	private String dir;

	private int retainCount;

	private String nameServerAddr;

	private String producerId;

	private String topic;

	private String application;

	private static final ExtensionLoader<FluxStatExporter> loader = ExtensionLoader.getExtensionLoader(FluxStatExporter.class);

	/**
	 * 使用url中的流量统计相关属性来构造Collector<br>
	 * 本类为单例，传入的url仅在第一次调用该函数时(即构造单例时)用到，暂不支持在单例构造完成后通过配置规则更新属性
	 */
	public static FluxStatCollector getCollector(URL url) {
		if (collectorHolder.get() == null) {
			synchronized (collectorHolder) {
				if (collectorHolder.get() == null) {
					collectorHolder.set(new FluxStatCollector(url));
				}
			}
		}
		return collectorHolder.get();
	}

	public FluxStatCollector(URL url) {
		final String[] fluxModes;
		if (url.getParameter(Constants.FLUX_STAT_ENABLED_KEY) != null) {
			this.interval = url.getPositiveParameter(Constants.FLUX_STAT_INTERVAL_KEY, Constants.FLUX_STAT_DEFAULT_INTERVAL);
			this.dir = url.getParameter(Constants.FLUX_STAT_DIR_KEY, System.getProperty("user.home"));
			fluxModes = url.getParameter(Constants.FLUX_STAT_MODE_KEY, Constants.FLUX_STAT_DEFAULT_MODE).split(",");
			this.retainCount = url.getPositiveParameter(Constants.FLUX_STAT_RETAIN_COUNT_KEY, Constants.FLUX_STAT_DEFAULT_RETAIN_COUNT);

			// 发送指标到ZCM用到的配置项
			this.application = url.getParameter(Constants.APPLICATION_KEY);
			this.nameServerAddr = url.getParameter(Constants.FLUX_STAT_ZCM_NAME_SERVER_ADDR_KEY);
			this.producerId = url.getParameter(Constants.FLUX_STAT_ZCM_PRODUCER_ID_KEY, this.application);
			this.topic = url.getParameter(Constants.FLUX_STAT_ZCM_TOPIC_KEY, Constants.FLUX_STAT_ZCM_DEFAULT_TOPIC);
		} else {
			// 兼容老版本配置项
			String intervalStr = ConfigUtils.getProperty(Constants.OUTDATED_FLUX_STAT_INTERVAL_KEY, String.valueOf(Constants.FLUX_STAT_DEFAULT_INTERVAL));
			this.interval = Long.parseLong(intervalStr) > 0 ? Long.parseLong(intervalStr) : Constants.FLUX_STAT_DEFAULT_INTERVAL;
			this.dir = ConfigUtils.getProperty(Constants.OUTDATED_FLUX_STAT_DIR_KEY, System.getProperty("user.home"));
			fluxModes = ConfigUtils.getProperty(Constants.OUTDATED_FLUX_STAT_MODE_KEY, Constants.FLUX_STAT_DEFAULT_MODE).split(",");
			this.retainCount = Constants.FLUX_STAT_DEFAULT_RETAIN_COUNT;
		}

		// 初始化线程池统计器
		Map<String, String> param = new HashMap<String, String>();
		param.put(Constants.FLUX_STAT_INTERVAL_KEY, Long.toString(interval));
		ExtensionLoader.getExtensionLoader(StatCollector.class).getExtension(Constants.THREADPOOL_KEY).init(param);

		scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {

			long lastStatTime = System.currentTimeMillis() / interval;

			public void run() {
				// 看当前时间和上次统计时间相差几个interval，防止数据丢失
				long intervalNum = System.currentTimeMillis() / interval - lastStatTime;
				for (int i = 0; i < intervalNum; i++) {
					long statTime = lastStatTime + i;

					Map<FluxStatKey, long[]> tempStatisticsMap = new HashMap<FluxStatKey, long[]>();
					for (Entry<FluxStatKey, LoadingCache<Long, AtomicReference<long[]>>> entry : statisticsMap.entrySet()) {
						FluxStatKey statKey = entry.getKey();
						LoadingCache<Long, AtomicReference<long[]>> cache = entry.getValue();
						AtomicReference<long[]> statisticsReference = cache.getIfPresent(statTime);
						if (statisticsReference == null) {
							continue;
						}
						long[] statistics = statisticsReference.get();
						if (statistics == null) {
							continue;
						}

						// 对应时段的线程池拒绝数
						StatCollector statCollector = ExtensionLoader.getExtensionLoader(StatCollector.class).getExtension(Constants.THREADPOOL_KEY);
						Map<String, String> param = new HashMap<String, String>();
						param.put(Constants.FLUX_STAT_SERVICE_KEY, statKey.getUrl().getServiceKey());
						param.put(Constants.FLUX_STAT_TIME_KEY, Long.toString(statTime));
						long threadPoolRejection = (Long) statCollector.get(param);
						statistics = Arrays.copyOf(statistics, statistics.length + 1);
						if (threadPoolRejection > 0) {
							// 把线程池拒绝数放到数据末尾
							statistics[statistics.length - 1] = threadPoolRejection;
							// 将线程池拒绝数加入到failure
							statistics[1] += threadPoolRejection;
						}

						tempStatisticsMap.put(statKey, statistics);
					}
					if (tempStatisticsMap.size() > 0) {
						for (String mode : fluxModes) {
							try {
								loader.getExtension(mode).export(tempStatisticsMap, statTime);
							} catch (Throwable t) {
								logger.error("Failed to export flux statistics, cause: " + t.getMessage(), t);
							}
						}
					}
				}
				lastStatTime = lastStatTime + intervalNum;
			}
		}, interval, interval, TimeUnit.MILLISECONDS);
	}

	public static LoadingCache<Long, AtomicReference<long[]>> getCache(FluxStatKey key, long duration) {
		LoadingCache<Long, AtomicReference<long[]>> counter = statisticsMap.get(key);
		if (null == counter) {
			counter = CacheBuilder.newBuilder().expireAfterWrite(duration * 3, TimeUnit.MILLISECONDS).build(new CacheLoader<Long, AtomicReference<long[]>>() {
				public AtomicReference<long[]> load(Long key) throws Exception {
					return new AtomicReference<long[]>();
				}
			});
			LoadingCache<Long, AtomicReference<long[]>> existingCounter = statisticsMap.putIfAbsent(key, counter);
			if (null != existingCounter) {
				counter.cleanUp();
				counter = existingCounter;
			}
		}
		return counter;
	}

	public static void collect(URL url) throws ExecutionException {
		int success = url.getParameter(SUCCESS, 0);
		int failure = url.getParameter(FAILURE, 0);
		int timeout = url.getParameter(TIMEOUT, 0);
		int overload = url.getParameter(OVERLOAD, 0);
		int noprovider = url.getParameter(NO_PROVIDER, 0);
		int elapse = url.getParameter(ELAPSE, 0);
		int serviceConcurrent = url.getParameter(SERVICE_CONCURRENT, 0);
		int appConcurrent = url.getParameter(APP_CONCURRENT, 0);
		long timestamp = url.getParameter(TIMESTAMP, 0L);

		// 初始化原子引用
		FluxStatKey statKey = new FluxStatKey(url);
		LoadingCache<Long, AtomicReference<long[]>> cache = getCache(statKey, interval);
		long currentTime = timestamp / interval;
		AtomicReference<long[]> reference = cache.get(currentTime);
		long[] current;
		long[] update = new long[LENGTH];
		do {
			current = reference.get();
			if (current == null) {
				update[0] = success;
				update[1] = failure;
				update[2] = timeout;
				update[3] = elapse;
				update[4] = elapse;
				update[5] = serviceConcurrent;
				update[6] = appConcurrent;
				update[7] = elapse; // min elapse
				update[8] = overload;
				update[9] = noprovider;
			} else {
				update[0] = current[0] + success;
				update[1] = current[1] + failure;
				update[2] = current[2] + timeout;
				update[3] = current[3] + elapse;
				update[4] = current[4] > elapse ? current[4] : elapse;
				update[5] = current[5] > serviceConcurrent ? current[5] : serviceConcurrent;
				update[6] = current[6] > appConcurrent ? current[6] : appConcurrent;
				update[7] = current[7] > elapse ? elapse : current[7];
				update[8] = current[8] + overload;
				update[9] = current[9] + noprovider;
			}
		} while (!reference.compareAndSet(current, update));
	}

	public String getDir() {
		return dir;
	}

	public long getInterval() {
		return interval;
	}

	public int getRetainCount() {
		return retainCount;
	}

	public String getNameServerAddr() {
		return nameServerAddr;
	}

	public String getProducerId() {
		return producerId;
	}

	public String getTopic() {
		return topic;
	}

	public String getApplication() {
		return application;
	}

}
