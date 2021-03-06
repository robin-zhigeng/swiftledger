package com.higgschain.trust.contract.rhino;

import com.higgschain.trust.contract.ExecuteConfig;
import com.higgschain.trust.contract.rhino.types.BigDecimalWrap;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * The type Safe native java object.
 *
 * @author duhongming
 * @date 2018 /6/8
 */
public class SafeNativeJavaObject extends NativeJavaObject {

    /**
     * Instantiates a new Safe native java object.
     *
     * @param scope      the scope
     * @param javaObject the java object
     * @param staticType the static type
     */
    public SafeNativeJavaObject(final Scriptable scope, final Object javaObject, final Class<?> staticType) {
        super(scope, javaObject, staticType);
    }

    @Override
    public Object get(final String name, final Scriptable start) {
        boolean _equals = "getClass".equals(name);
        if (_equals && !ExecuteConfig.DEBUG) {
            return Scriptable.NOT_FOUND;
        }

        if (name.equals("length")) {
            if (unwrap() instanceof List) {
                List list = (List) unwrap();
                return list.size();
            }
        }
        Object val = unwrap();
        if (val instanceof Map) {
            Map mapVal = (Map) val;
            Object result = mapVal.containsKey(name) ? mapVal.get(name) : super.get(name, start);
            if (result instanceof BigDecimal) {
                return new BigDecimalWrap((BigDecimal) result);
            }
            if (result instanceof BigInteger) {
                return new BigDecimalWrap((BigInteger) result);
            }
            return result;
        }

        return super.get(name, start);
    }

    @Override
    public Object get(final int index, final Scriptable start) {
        Object obj = unwrap();
        if (obj instanceof List) {
            return ((List) obj).get(index);
        }
        return super.get(index, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        Object val = unwrap();
        if (val instanceof Map) {
            Map mapVal = (Map) val;
            mapVal.put(name, value);
            return;
        }
        super.put(name, start, value);
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (javaObject instanceof Number) {
            return ((Number) javaObject).doubleValue();
        }
        return super.getDefaultValue(hint);
    }

    @Override
    public String toString() {
        return javaObject.toString();
    }
}
