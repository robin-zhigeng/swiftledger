/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package com.higgschain.trust.evmcontract.net;

import java.util.BitSet;

/**
 * Created by Anton Nashatyrev on 24.09.2015.
 */
public class BloomFilter implements Cloneable {
    private static final int BITS_PER_BLOOM = 3;
    private static final int BLOOM_BYTES = 64;

    /**
     * The Mask.
     */
    BitSet mask = new BitSet(BLOOM_BYTES * 8);
    /**
     * The Counters.
     */
    int[] counters = new int[BLOOM_BYTES * 8];

    private BloomFilter() {
    }

    /**
     * Instantiates a new Bloom filter.
     *
     * @param topic the topic
     */
    public BloomFilter(Topic topic) {
        addTopic(topic);
    }

    /**
     * Instantiates a new Bloom filter.
     *
     * @param bloomMask the bloom mask
     */
    public BloomFilter(byte[] bloomMask) {
        if (bloomMask.length != BLOOM_BYTES)
            throw new RuntimeException("Invalid bloom filter array length: " + bloomMask.length);
        mask = BitSet.valueOf(bloomMask);
    }

    /**
     * Create none bloom filter.
     *
     * @return the bloom filter
     */
    public static BloomFilter createNone() {
        return new BloomFilter();
    }

    /**
     * Create all bloom filter.
     *
     * @return the bloom filter
     */
    public static BloomFilter createAll() {
        BloomFilter bloomFilter = new BloomFilter();
        bloomFilter.mask.set(0, bloomFilter.mask.length());
        return bloomFilter;
    }

    private void incCounters(BitSet bs) {
        int idx = -1;
        while (true) {
            idx = bs.nextSetBit(idx + 1);
            if (idx < 0) break;
            counters[idx]++;
        }
    }

    private void decCounters(BitSet bs) {
        int idx = -1;
        while (true) {
            idx = bs.nextSetBit(idx + 1);
            if (idx < 0) break;
            if (counters[idx] > 0) counters[idx]--;
        }
    }

    private BitSet getTopicMask(Topic topic) {
        BitSet topicMask = new BitSet(BLOOM_BYTES * 8);
        for (int i = 0; i < BITS_PER_BLOOM; i++) {
            int x = topic.getBytes()[i] & 0xFF;
            if ((topic.getBytes()[BITS_PER_BLOOM] & (1 << i)) != 0) {
                x += 256;
            }
            topicMask.set(x);
        }
        return topicMask;
    }

    /**
     * Add topic.
     *
     * @param topic the topic
     */
    public void addTopic(Topic topic) {
        BitSet topicMask = getTopicMask(topic);
        incCounters(topicMask);
        mask.or(topicMask);
    }

    /**
     * Remove topic.
     *
     * @param topic the topic
     */
    public void removeTopic(Topic topic) {
        BitSet topicMask = getTopicMask(topic);
        decCounters(topicMask);
        int idx = -1;
        while (true) {
            idx = topicMask.nextSetBit(idx + 1);
            if (idx < 0) break;
            if (counters[idx] == 0) mask.clear(idx);
        }
    }

    /**
     * Has topic boolean.
     *
     * @param topic the topic
     * @return the boolean
     */
    public boolean hasTopic(Topic topic) {
        BitSet m = new BloomFilter(topic).mask;
        BitSet m1 = (BitSet) m.clone();
        m1.and(mask);
        return m1.equals(m);
    }

    /**
     * To bytes byte [ ].
     *
     * @return the byte [ ]
     */
    public byte[] toBytes() {
        byte[] ret = new byte[BLOOM_BYTES];
        byte[] bytes = mask.toByteArray();
        System.arraycopy(bytes, 0, ret, 0, bytes.length);
        return ret;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BloomFilter && mask.equals(((BloomFilter) obj).mask);

    }

    @Override
    protected BloomFilter clone() throws CloneNotSupportedException {
        try {
            return (BloomFilter) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
