package com.higgschain.trust.slave.metrics.jvm;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.RatioGauge;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * The type Memory usage gauge set.
 *
 * @author duhongming
 * @date 2018 /12/28
 */
public class MemoryUsageGaugeSet implements MetricSet {
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    private final MemoryMXBean mxBean;
    private final List<MemoryPoolMXBean> memoryPools;

    /**
     * Instantiates a new Memory usage gauge set.
     */
    public MemoryUsageGaugeSet() {
        this(ManagementFactory.getMemoryMXBean(), ManagementFactory.getMemoryPoolMXBeans());
    }

    /**
     * Instantiates a new Memory usage gauge set.
     *
     * @param memoryPools the memory pools
     */
    public MemoryUsageGaugeSet(Collection<MemoryPoolMXBean> memoryPools) {
        this(ManagementFactory.getMemoryMXBean(), memoryPools);
    }

    /**
     * Instantiates a new Memory usage gauge set.
     *
     * @param mxBean      the mx bean
     * @param memoryPools the memory pools
     */
    public MemoryUsageGaugeSet(MemoryMXBean mxBean, Collection<MemoryPoolMXBean> memoryPools) {
        this.mxBean = mxBean;
        this.memoryPools = new ArrayList<>(memoryPools);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();

        gauges.put("total.init", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getInit() + mxBean.getNonHeapMemoryUsage().getInit());
        gauges.put("total.used", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getUsed() + mxBean.getNonHeapMemoryUsage().getUsed());
        gauges.put("total.max", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getMax() + mxBean.getNonHeapMemoryUsage().getMax());
        gauges.put("total.committed", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getCommitted() + mxBean.getNonHeapMemoryUsage().getCommitted());

        gauges.put("heap.init", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getInit());
        gauges.put("heap.used", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getUsed());
        gauges.put("heap.max", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getMax());
        gauges.put("heap.committed", (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getCommitted());
        gauges.put("heap.usage", new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                final MemoryUsage usage = mxBean.getHeapMemoryUsage();
                return Ratio.of(usage.getUsed(), usage.getMax());
            }
        });

        gauges.put("non-heap.init", (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getInit());
        gauges.put("non-heap.used", (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getUsed());
        gauges.put("non-heap.max", (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getMax());
        gauges.put("non-heap.committed", (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getCommitted());
        gauges.put("non-heap.usage", new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                final MemoryUsage usage = mxBean.getNonHeapMemoryUsage();
                return Ratio.of(usage.getUsed(), usage.getMax());
            }
        });

        for (final MemoryPoolMXBean pool : memoryPools) {
            final String poolName = name("pools", WHITESPACE.matcher(pool.getName()).replaceAll("-"));

            gauges.put(name(poolName, "usage"), new RatioGauge() {
                @Override
                protected Ratio getRatio() {
                    MemoryUsage usage = pool.getUsage();
                    return Ratio.of(usage.getUsed(), usage.getMax() == -1 ? usage.getCommitted() : usage.getMax());
                }
            });

            gauges.put(name(poolName, "max"), (Gauge<Long>) () -> pool.getUsage().getMax());
            gauges.put(name(poolName, "used"), (Gauge<Long>) () -> pool.getUsage().getUsed());
            gauges.put(name(poolName, "committed"), (Gauge<Long>) () -> pool.getUsage().getCommitted());

            // Only register GC usage metrics if the memory pool supports usage statistics.
            if (pool.getCollectionUsage() != null) {
                gauges.put(name(poolName, "used-after-gc"), (Gauge<Long>) () -> pool.getCollectionUsage().getUsed());
            }

            gauges.put(name(poolName, "init"), (Gauge<Long>) () -> pool.getUsage().getInit());
        }

        return Collections.unmodifiableMap(gauges);
    }
}
