/*
 * Copyright (c) 2013-2017, suimi
 */
package com.higgschain.trust.consensus.config.listener;

import com.higgschain.trust.consensus.exception.ConsensusError;
import com.higgschain.trust.consensus.exception.ConsensusException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;

/**
 * The type State change listener adaptor.
 *
 * @author suimi
 * @date 2018 /6/13
 */
@Slf4j public class StateChangeListenerAdaptor implements Ordered {

    private Object bean;

    private Method method;

    private int order;

    @Getter private boolean before;

    /**
     * Instantiates a new State change listener adaptor.
     *
     * @param bean   the bean
     * @param method the method
     */
    public StateChangeListenerAdaptor(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
        Order ann = AnnotationUtils.findAnnotation(method, Order.class);
        order = ann != null ? ann.value() : Ordered.LOWEST_PRECEDENCE;
        StateChangeListener listener = AnnotationUtils.findAnnotation(method, StateChangeListener.class);
        before = listener.before();
    }

    /**
     * Invoke.
     */
    public void invoke() {
        try {
            log.debug("invoke the listener method:{}.{}", bean.getClass().getSimpleName(), method.getName());
            method.invoke(bean);
        } catch (Exception e) {
            throw new ConsensusException(ConsensusError.CONFIG_NODE_STATE_CHANGE_INVOKE_FAILED, e);
        }
    }

    @Override public int getOrder() {
        return order;
    }
}
