package org.apache.dubbo.flux;

import java.util.Map;

import org.apache.dubbo.common.extension.SPI;

@SPI
public interface FluxStatExporter {
	
	public void export(Map<FluxStatKey, long[]> data, long statTime);
}
