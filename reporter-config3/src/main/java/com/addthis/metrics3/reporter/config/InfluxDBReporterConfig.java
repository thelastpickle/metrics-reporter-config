/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.addthis.metrics3.reporter.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.addthis.metrics.reporter.config.AbstractInfluxDBReporterConfig;
import com.addthis.metrics.reporter.config.HostPort;
import com.codahale.metrics.MetricRegistry;
import com.izettle.metrics.influxdb.InfluxDbHttpSender;
import com.izettle.metrics.influxdb.InfluxDbReporter;
import com.izettle.metrics.influxdb.InfluxDbSender;

public class InfluxDBReporterConfig extends AbstractInfluxDBReporterConfig implements MetricsReporterConfigThree
{
    private static final Logger log = LoggerFactory.getLogger(InfluxDBReporterConfig.class);

    private static final String HOST_TAG_NAME = "host";

    private InfluxDbReporter reporter;

    private void enableMetrics3(HostPort hostPort, MetricRegistry registry) throws Exception
    {
        Map<String, String> tagsMap = new HashMap<String, String>();
        tagsMap.put(HOST_TAG_NAME, getResolvedTag());

        InfluxDbSender influxDbSender = new InfluxDbHttpSender(getProtocol(), hostPort.getHost(), hostPort.getPort(),
            getDbName(), getAuth(), getRealRateunit(), getConnectionTimeout(), getReadTimeout());

        reporter = InfluxDbReporter.forRegistry(registry).convertRatesTo(getRealRateunit())
            .convertDurationsTo(getRealDurationunit()).withTags(tagsMap)
            .filter(MetricFilterTransformer.generateFilter(getPredicate())).build(influxDbSender);

        reporter.start(getPeriod(), getRealTimeunit());
    }

    @Override
    public void report()
    {
        if (reporter != null) {
            reporter.report();
        }
    }

    @Override
    public boolean enable(final MetricRegistry registry)
    {
        boolean success = checkClass("com.izettle.metrics.influxdb.InfluxDbReporter");
        if (!success)
        {
            return false;
        }

        List<HostPort> hosts = getFullHostList();
        if (hosts == null || hosts.isEmpty())
        {
            log.error("No hosts specified, cannot enable InfluxDBReporter");
            return false;
        }

        for (HostPort hostPort : hosts)
        {
            log.info("Enabling InfluxDBReporter to {}:{}", hostPort.getHost(), hostPort.getPort());
            try
            {
                // connect with first working host
                enableMetrics3(hostPort, registry);
                return true;
            } catch (Exception e)
            {
                log.error("Failed to enable InfluxDBReporter for {}:{}", hostPort.getHost(), hostPort.getPort(), e);
            }
        }
        log.error("None of configured InfluxDBReporter hosts worked");
        return false;
    }

    private boolean checkClass(String className)
    {
        if (!isClassAvailable(className))
        {
            log.error("Tried to enable InfluxDBReporter, but class {} was not found", className);
            return false;
        } else
            {
            return true;
        }
    }
}
