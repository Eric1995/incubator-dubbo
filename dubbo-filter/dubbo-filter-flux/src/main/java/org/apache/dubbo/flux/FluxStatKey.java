package org.apache.dubbo.flux;

import java.io.Serializable;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;

public class FluxStatKey implements Serializable {
	
	private static final long serialVersionUID = -1010425205939951170L;
	
	public static final String APPLICATION = "application";

    public static final String INTERFACE = "interface";

    public static final String METHOD = "method";

    public static final String GROUP = "group";

    public static final String VERSION = "version";

    public static final String TIMESTAMP = "timestamp";

    public static final String SUCCESS = "success";

    public static final String FAILURE = "failure";
    
    public static final String TIMEOUT = "timeout";
    
    public static final String OVERLOAD = "overload";

    public static final String ELAPSE = "elapse";

    public static final String SERVICE_CONCURRENT = "service.concurrent";
    
    public static final String APP_CONCURRENT = "app.concurrent";

    public static final String NO_PROVIDER="noprovider";

	private URL url;

	private String application;

	private String service;

	private String group;

	private String version;
	
	private String side;

	public FluxStatKey(URL url) {
		this.url = url;
		this.application = url.getParameter(APPLICATION);
		this.service = url.getParameter(INTERFACE);
		this.group = url.getParameter(GROUP);
		this.version = url.getParameter(VERSION);
		this.side = url.getParameter(Constants.SIDE_KEY);
	}

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public String getApplication() {
		return application;
	}

	public FluxStatKey setApplication(String application) {
		this.application = application;
		return this;
	}

	public String getService() {
		return service;
	}

	public FluxStatKey setService(String service) {
		this.service = service;
		return this;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((application == null) ? 0 : application.hashCode());
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result + ((service == null) ? 0 : service.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		result = prime * result + ((side == null) ? 0 : side.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FluxStatKey other = (FluxStatKey) obj;
		if (application == null) {
			if (other.application != null)
				return false;
		} else if (!application.equals(other.application))
			return false;
		if (group == null) {
			if (other.group != null)
				return false;
		} else if (!group.equals(other.group))
			return false;
		if (service == null) {
			if (other.service != null)
				return false;
		} else if (!service.equals(other.service))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		if (side == null) {
            if (other.side != null)
                return false;
        } else if (!side.equals(other.side))
            return false;
		return true;
	}

	@Override
	public String toString() {
		return url.toString();
	}

}
