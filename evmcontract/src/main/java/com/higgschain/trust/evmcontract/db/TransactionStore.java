package com.higgschain.trust.evmcontract.db;


import com.higgschain.trust.evmcontract.core.TransactionResultInfo;
import com.higgschain.trust.evmcontract.datasource.ObjectDataSource;
import com.higgschain.trust.evmcontract.datasource.Serializer;
import com.higgschain.trust.evmcontract.datasource.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Transaction store.
 *
 * @author duhongming
 * @date 2018 /12/4
 */
public class TransactionStore extends ObjectDataSource<TransactionResultInfo> {
    private static final Logger logger = LoggerFactory.getLogger("db");

    private final static Serializer<TransactionResultInfo, byte[]> SERIALIZER =
            new Serializer<TransactionResultInfo, byte[]>() {
                @Override
                public byte[] serialize(TransactionResultInfo txResultInfo) {
                    return txResultInfo.getEncoded();
                }

                @Override
                public TransactionResultInfo deserialize(byte[] stream) {
                    try {
                        if (stream == null) {
                            return null;
                        }
                        return new TransactionResultInfo(stream);
                    } catch (Exception e) {
                        logger.error("Deserialize TransactionResultInfo error: {}", e.getMessage());
                        return null;
                    }
                }
            };

    /**
     * Instantiates a new Transaction store.
     *
     * @param db the db
     */
    public TransactionStore(Source<byte[], byte[]> db) {
        super(db, SERIALIZER, 256);
    }
}

