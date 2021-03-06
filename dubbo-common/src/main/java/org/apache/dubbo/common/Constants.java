/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.common;


import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;

/**
 * Constants
 */
public class Constants {




    // BEGIN dubbo-config-api
    public static final String CLUSTER_KEY = "cluster";

    public static final String STATUS_KEY = "status";

    public static final String CONTEXTPATH_KEY = "contextpath";

    public static final String LISTENER_KEY = "listener";

    public static final String LAYER_KEY = "layer";

    /**
     * General
     */
    /**
     * Application name;
     */
    public static final String NAME = "name";

    /**
     * Application owner name;
     */
    public static final String OWNER = "owner";

    /**
     * Running application organization name.
     */
    public static final String ORGANIZATION = "organization";

    /**
     * Application architecture name.
     */
    public static final String ARCHITECTURE = "architecture";

    /**
     * Environment name
     */
    public static final String ENVIRONMENT = "environment";

    /**
     * Test environment key.
     */
    public static final String TEST_ENVIRONMENT = "test";

    /**
     * Development environment key.
     */
    public static final String DEVELOPMENT_ENVIRONMENT = "develop";

    /**
     * Production environment key.
     */
    public static final String PRODUCTION_ENVIRONMENT = "product";

    public static final String CONFIG_CLUSTER_KEY = "config.cluster";
    public static final String CONFIG_NAMESPACE_KEY = "config.namespace";
    public static final String CONFIG_GROUP_KEY = "config.group";
    public static final String CONFIG_CHECK_KEY = "config.check";

    public static final String CONFIG_CONFIGFILE_KEY = "config.config-file";
    public static final String CONFIG_ENABLE_KEY = "config.highest-priority";
    public static final String CONFIG_TIMEOUT_KEY = "config.timeout";
    public static final String CONFIG_APPNAME_KEY = "config.app-name";

    public static final String USERNAME_KEY = "username";

    public static final String PASSWORD_KEY = "password";

    public static final String HOST_KEY = "host";

    public static final String PORT_KEY = "port";

    public static final String MULTICAST = "multicast";

    public static final String REGISTER_IP_KEY = "register.ip";

    public static final String DUBBO_IP_TO_REGISTRY = "DUBBO_IP_TO_REGISTRY";

    public static final String DUBBO_PORT_TO_REGISTRY = "DUBBO_PORT_TO_REGISTRY";

    public static final String DUBBO_IP_TO_BIND = "DUBBO_IP_TO_BIND";

    public static final String DUBBO_PORT_TO_BIND = "DUBBO_PORT_TO_BIND";

    public static final String SCOPE_KEY = "scope";

    public static final String SCOPE_LOCAL = "local";

    public static final String SCOPE_REMOTE = "remote";

    public static final String SCOPE_NONE = "none";

    public static final String ON_CONNECT_KEY = "onconnect";

    public static final String ON_DISCONNECT_KEY = "ondisconnect";

    public static final String ON_INVOKE_METHOD_KEY = "oninvoke.method";

    public static final String ON_RETURN_METHOD_KEY = "onreturn.method";

    public static final String ON_THROW_METHOD_KEY = "onthrow.method";

    public static final String ON_INVOKE_INSTANCE_KEY = "oninvoke.instance";

    public static final String ON_RETURN_INSTANCE_KEY = "onreturn.instance";

    public static final String ON_THROW_INSTANCE_KEY = "onthrow.instance";

    @Deprecated
    public static final String SHUTDOWN_WAIT_SECONDS_KEY = "dubbo.service.shutdown.wait.seconds";

    public static final String SHUTDOWN_WAIT_KEY = "dubbo.service.shutdown.wait";

    /**
     * The key name for export URL in register center
     */
    public static final String EXPORT_KEY = "export";

    /**
     * The key name for reference URL in register center
     */
    public static final String REFER_KEY = "refer";

    /**
     * To decide whether to make connection when the client is created
     */
    public static final String LAZY_CONNECT_KEY = "lazy";

    public static final String DUBBO_PROTOCOL = DUBBO;

    public static final String ZOOKEEPER_PROTOCOL = "zookeeper";

    // FIXME: is this still useful?
    public static final String SHUTDOWN_TIMEOUT_KEY = "shutdown.timeout";

    public static final int DEFAULT_SHUTDOWN_TIMEOUT = 1000 * 60 * 15;

    public static final String PROTOCOLS_SUFFIX = "dubbo.protocols.";

    public static final String PROTOCOL_SUFFIX = "dubbo.protocol.";

    public static final String REGISTRIES_SUFFIX = "dubbo.registries.";

    public static final String TELNET = "telnet";

    public static final String QOS_ENABLE = "qos.enable";

    public static final String QOS_PORT = "qos.port";

    public static final String ACCEPT_FOREIGN_IP = "qos.accept.foreign.ip";
    // END dubbo-congfig-api

    // BEGIN dubbo-metadata-report-api
    public static final String METADATA_REPORT_KEY = "metadata";

    public static final String RETRY_TIMES_KEY = "retry.times";

    public static final Integer DEFAULT_METADATA_REPORT_RETRY_TIMES = 100;

    public static final String RETRY_PERIOD_KEY = "retry.period";

    public static final Integer DEFAULT_METADATA_REPORT_RETRY_PERIOD = 3000;

    public static final String SYNC_REPORT_KEY = "sync.report";

    public static final String CYCLE_REPORT_KEY = "cycle.report";

    public static final Boolean DEFAULT_METADATA_REPORT_CYCLE_REPORT = true;
    // END dubbo-metadata-report-api

    // BEGIN dubbo-filter-cache
    public static final String CACHE_KEY = "cache";
    // END dubbo-filter-cache


    // BEGIN dubbo-filter-validation
    public static final String VALIDATION_KEY = "validation";
    // END dubbo-filter-validation

    public static final String DEFAULT_CLUSTER = "failover";

    /**
     * public static final int DEFAULT_REGISTRY_CONNECT_TIMEOUT = 5000;
     */

    public static final String DIRECTORY_KEY = "directory";

    public static final String ASYNC_SUFFIX = "Async";

    public static final String WAIT_KEY = "wait";

    public static final String HESSIAN_VERSION_KEY = "hessian.version";


    public static final String CONFIG_PROTOCOL = "config";


    public static final String RELIABLE_PROTOCOL = "napoli";
}
