package com.higgschain.trust.slave.api;

import java.util.List;

/**
 * The interface Contract query service.
 *
 * @author duhongming
 * @date 2018 /6/22
 */
public interface ContractQueryService {
    /**
     * Queries contract state.
     *
     * @param address    the address
     * @param methodName the method name
     * @param args       the args
     * @return object
     */
    Object query(String address, String methodName, Object... args);

    /**
     * Queries contract state.
     *
     * @param blockHeight     block height
     * @param contractAddress contract address
     * @param methodSignature method signature written with target language
     * @param methodInputArgs actual parameters
     * @return result returned by contract invocation
     */
    List<?> query2(Long blockHeight, String contractAddress, String methodSignature, Object... methodInputArgs);

    /**
     * Queries contract state.
     *
     * @param contractAddress contract address
     * @param methodSignature method signature written with target language
     * @param methodInputArgs actual parameters
     * @return result returned by contract invocation
     */
    default List<?> query2(String contractAddress, String methodSignature, Object... methodInputArgs) {
        return query2(-1L, contractAddress, methodSignature, methodInputArgs);
    }
}
