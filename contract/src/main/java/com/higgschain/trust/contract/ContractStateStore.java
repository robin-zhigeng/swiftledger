package com.higgschain.trust.contract;

/**
 * contract state key-value db store interface
 *
 * @author duhongming
 * @date 2018 -04-09
 */
public interface ContractStateStore {
    /**
     * store contract state
     *
     * @param key   the key
     * @param state the state
     */
    void put(String key, Object state);

    /**
     * get by key
     *
     * @param key the key
     * @return value object
     */
    Object get(String key);

    /**
     * remove by key
     *
     * @param key the key
     */
    void remove(String key);
}
