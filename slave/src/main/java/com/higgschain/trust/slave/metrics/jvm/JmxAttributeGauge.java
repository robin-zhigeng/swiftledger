package com.higgschain.trust.slave.metrics.jvm;

import com.codahale.metrics.Gauge;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Set;

/**
 * @author duhongming
 * @date 2018/12/28
 */
public class JmxAttributeGauge implements Gauge<Object> {
    private final MBeanServerConnection mBeanServerConn;
    private final ObjectName objectName;
    private final String attributeName;

    /**
     * Creates a new JmxAttributeGauge.
     *
     * @param objectName    the name of the object
     * @param attributeName the name of the object's attribute
     */
    public JmxAttributeGauge(ObjectName objectName, String attributeName) {
        this(ManagementFactory.getPlatformMBeanServer(), objectName, attributeName);
    }

    /**
     * Creates a new JmxAttributeGauge.
     *
     * @param mBeanServerConn the {@link MBeanServerConnection}
     * @param objectName      the name of the object
     * @param attributeName   the name of the object's attribute
     */
    public JmxAttributeGauge(MBeanServerConnection mBeanServerConn, ObjectName objectName, String attributeName) {
        this.mBeanServerConn = mBeanServerConn;
        this.objectName = objectName;
        this.attributeName = attributeName;
    }

    @Override
    public Object getValue() {
        try {
            return mBeanServerConn.getAttribute(getObjectName(), attributeName);
        } catch (IOException | JMException e) {
            return null;
        }
    }

    private ObjectName getObjectName() throws IOException {
        if (objectName.isPattern()) {
            Set<ObjectName> foundNames = mBeanServerConn.queryNames(objectName, null);
            if (foundNames.size() == 1) {
                return foundNames.iterator().next();
            }
        }
        return objectName;
    }

    public static void main(String[] args) throws MalformedObjectNameException {
        JmxAttributeGauge gauge = new JmxAttributeGauge(new ObjectName("java.nio:type=BufferPool,name=direct"), "MemoryUsed");
        System.out.println(gauge.getValue());
    }
}