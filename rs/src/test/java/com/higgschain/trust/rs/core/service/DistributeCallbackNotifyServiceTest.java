package com.higgschain.trust.rs.core.service;

import com.higgschain.trust.IntegrateBaseTest;
import com.higgschain.trust.rs.core.api.CoreTransactionService;
import com.higgschain.trust.rs.core.api.DistributeCallbackNotifyService;
import com.higgschain.trust.rs.core.api.enums.RedisMegGroupEnum;
import com.higgschain.trust.rs.core.api.enums.RedisTopicEnum;
import com.higgschain.trust.rs.core.callback.SlaveBatchCallbackProcessor;
import com.higgschain.trust.common.vo.RespData;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.concurrent.TimeUnit;

/**
 * DistributeCallbackNotifyService test
 *
 * @author lingchao
 * @create 2018年08月27日1 :12
 */
public class DistributeCallbackNotifyServiceTest extends IntegrateBaseTest {
    @Autowired
    private DistributeCallbackNotifyService distributeCallbackNotifyService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private SlaveBatchCallbackProcessor slaveBatchCallbackProcessor;
    @Autowired
    private CoreTransactionService coreTransactionService;

    /**
     * Test.
     *
     * @throws Exception the exception
     */
    @Test
    public void test() throws Exception {
        int num = 200;
        RedisMegGroupEnum redisMegGroupEnum = null;
        for (int i = 0; i < num; i++) {
            redisMegGroupEnum = i % 2 == 0 ? RedisMegGroupEnum.ON_PERSISTED_CALLBACK_MESSAGE_NOTIFY : RedisMegGroupEnum.ON_CLUSTER_PERSISTED_CALLBACK_MESSAGE_NOTIFY;
            Thread thread = new Thread(new WaitAndNotify(redisMegGroupEnum, i + ""));
            thread.start();
        }

        for (int i = 0; i < num; i++) {
            redisMegGroupEnum = i % 2 == 0 ? RedisMegGroupEnum.ON_PERSISTED_CALLBACK_MESSAGE_NOTIFY : RedisMegGroupEnum.ON_CLUSTER_PERSISTED_CALLBACK_MESSAGE_NOTIFY;
            Thread thread = new Thread(new Notify(redisMegGroupEnum, i + ""));
            thread.start();
        }
        Thread.sleep(10000000);


    }

    /**
     * The type Wait and notify.
     */
    public class WaitAndNotify implements Runnable {
        private RedisMegGroupEnum redisMegGroupEnum;
        private String txId;

        /**
         * Instantiates a new Wait and notify.
         *
         * @param redisMegGroupEnum the redis meg group enum
         * @param txId              the tx id
         */
        WaitAndNotify(RedisMegGroupEnum redisMegGroupEnum, String txId) {
            this.redisMegGroupEnum = redisMegGroupEnum;
            this.txId = txId;
        }

        public void run() {
            txId = txId +  (redisMegGroupEnum == RedisMegGroupEnum.ON_PERSISTED_CALLBACK_MESSAGE_NOTIFY ? " On persisted " : " On cluster ");
            System.out.println("txId = " + distributeCallbackNotifyService.syncWaitNotify(txId, redisMegGroupEnum, 10000, TimeUnit.SECONDS).getData());
        }
    }

    /**
     * The type Notify.
     */
    public class Notify implements Runnable {
        private RedisMegGroupEnum redisMegGroupEnum;
        private String txId;

        /**
         * Instantiates a new Notify.
         *
         * @param redisMegGroupEnum the redis meg group enum
         * @param txId              the tx id
         */
        Notify(RedisMegGroupEnum redisMegGroupEnum, String txId) {
            this.redisMegGroupEnum = redisMegGroupEnum;
            this.txId = txId;
        }

        public void run() {
            txId = txId +  (redisMegGroupEnum == RedisMegGroupEnum.ON_PERSISTED_CALLBACK_MESSAGE_NOTIFY ? " On persisted " : " On cluster ");
            RespData respData = new RespData();
            respData.setData(txId);
            distributeCallbackNotifyService.notifySyncResult(Lists.newArrayList(respData), redisMegGroupEnum);
        }
    }

    /**
     * Process init tx.
     */
    @Test
    public void processInitTx(){
        RTopic<String> topic = redissonClient.getTopic(RedisTopicEnum.ASYNC_TO_PROCESS_INIT_TX.getCode());
        topic.publish("lingchao");
    }


}
