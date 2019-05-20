package org.apache.dubbo.flux;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Activate(group = {Constants.PROVIDER,Constants.CONSUMER})
public class FluxStatFilter implements  Filter{

	private static final Logger logger = LoggerFactory.getLogger(FluxStatFilter.class);

	private static final int STATUS_SUCCESS = 1;

	private static final int STATUS_FAILURE = 2;

	private static final int STATUS_TIMEOUT = 3;

	private static final int STATUS_OVERLOAD = 4;

	private final ConcurrentMap<String, AtomicInteger> serviceConcurrentCounters = new ConcurrentHashMap<String, AtomicInteger>();

	private final AtomicInteger appConcurrentCounter = new AtomicInteger();

	private Boolean oldVersionFluxSwitch;

	public FluxStatFilter() {
		this.oldVersionFluxSwitch = Boolean.valueOf(ConfigUtils.getProperty(Constants.OUTDATED_FLUX_STAT_ENABLED_KEY));
	}

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		URL url = invoker.getUrl();
		boolean fluxEnabled = false;
		String fluxSwitch = url.getParameter(Constants.FLUX_STAT_ENABLED_KEY);
		if (fluxSwitch != null) {
			fluxEnabled = Boolean.valueOf(fluxSwitch);
		} else {
			// 兼容老版本配置项
			fluxEnabled = oldVersionFluxSwitch;
		}
		if (fluxEnabled) {
			RpcContext context = RpcContext.getContext(); // 提供方必须在invoke()之前获取context信息
			long start = System.currentTimeMillis(); // 记录起始时间戳
			getServiceConcurrentCounter(invoker).incrementAndGet(); // 服务并发计数
			appConcurrentCounter.incrementAndGet(); // 进程并发计数
			try {
				Result result = invoker.invoke(invocation); // 让调用链往下执行
				collect(url, invoker, invocation, result, context, start, STATUS_SUCCESS);
				return result;
			} catch (RpcException e) {
				if (e.isTimeout()) {
					collect(url, invoker, invocation, null, context, start, STATUS_TIMEOUT);
				} else if (e.isFluxLimit()) {
					collect(url, invoker, invocation, null, context, start, STATUS_OVERLOAD);
				} else {
					collect(url, invoker, invocation, null, context, start, STATUS_FAILURE);
				}
				throw e;
			} finally {
				getServiceConcurrentCounter(invoker).decrementAndGet(); // 服务并发计数
				appConcurrentCounter.decrementAndGet(); // 进程并发计数
			}
		} else {
			return invoker.invoke(invocation);
		}
	}

	private void collect(URL url, Invoker<?> invoker, Invocation invocation, Result result, RpcContext context, long startTime, int status) {
		try {
			FluxStatCollector collector = FluxStatCollector.getCollector(url);
			long completionTime = System.currentTimeMillis();
			long elapsed = completionTime - startTime; // 计算调用耗时
			int serviceConcurrent = getServiceConcurrentCounter(invoker).get();
			int appConcurrent = appConcurrentCounter.get();
			String application = invoker.getUrl().getParameter(Constants.APPLICATION_KEY);
			String service = invoker.getInterface().getName(); // 获取服务名称
			String method = RpcUtils.getMethodName(invocation); // 获取方法名
			int localPort;
			if (Constants.CONSUMER_SIDE.equals(invoker.getUrl().getParameter(Constants.SIDE_KEY))) {
				// ---- 服务消费方监控 ----
				localPort = 0;
//				Throwable exception=result.getException();
			} else {
				// ---- 服务提供方监控 ----
				localPort = invoker.getUrl().getPort();
			}


			collector.collect(new URL(invoker.getUrl().getProtocol(), NetUtils.getLocalHost(), localPort, service + "/" + method, //
					FluxStatKey.APPLICATION, application, //
					FluxStatKey.INTERFACE, service, //
					FluxStatKey.METHOD, method, //
					Constants.SIDE_KEY, invoker.getUrl().getParameter(Constants.SIDE_KEY), //
					FluxStatKey.GROUP, invoker.getUrl().getParameter(Constants.GROUP_KEY), //
					FluxStatKey.VERSION, invoker.getUrl().getParameter(Constants.VERSION_KEY),
					FluxStatKey.TIMESTAMP, String.valueOf(completionTime), //
					status == 1 ? FluxStatKey.SUCCESS : FluxStatKey.FAILURE, "1", //
					FluxStatKey.TIMEOUT, (status == STATUS_TIMEOUT) ? "1" : "0", //
					FluxStatKey.OVERLOAD, (status == STATUS_OVERLOAD) ? "1" : "0", //
					FluxStatKey.ELAPSE, String.valueOf(elapsed), //
					FluxStatKey.SERVICE_CONCURRENT, String.valueOf(serviceConcurrent), //
					FluxStatKey.APP_CONCURRENT, String.valueOf(appConcurrent), //
					FluxStatKey.NO_PROVIDER,String.valueOf(0) //
			));
		} catch (Throwable t) {
			logger.error("Failed to collect flux statistics for service " + invoker.getUrl() + ", cause: " + t.getMessage(), t);
		}
	}

	private AtomicInteger getServiceConcurrentCounter(Invoker<?> invoker) {
		String service = invoker.getInterface().getName();
		AtomicInteger concurrentCounter = serviceConcurrentCounters.get(service);
		if (concurrentCounter == null) {
			serviceConcurrentCounters.putIfAbsent(service, new AtomicInteger());
			concurrentCounter = serviceConcurrentCounters.get(service);
		}
		return concurrentCounter;
	}
}
