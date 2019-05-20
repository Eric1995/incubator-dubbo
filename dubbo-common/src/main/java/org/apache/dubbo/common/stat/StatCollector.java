package org.apache.dubbo.common.stat;

import org.apache.dubbo.common.extension.SPI;

import java.util.Map;

@SPI
public interface StatCollector {
    
    public void collect(Object obj);
    
    public Object get(Map<String, String> param);
    
    public void init(Map<String, String> param);
    
}
