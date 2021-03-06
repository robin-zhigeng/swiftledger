package com.higgschain.trust.slave.model.bo.manage;

import com.higgschain.trust.slave.core.service.snapshot.agent.MerkleTreeSnapshotAgent;
import com.higgschain.trust.slave.model.bo.BaseBO;
import com.higgschain.trust.slave.model.enums.biz.RsNodeStatusEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The type Rs node.
 *
 * @author tangfashuang
 * @date 2018 /04/12 18:24
 * @desc rs pubKey BO
 */
@Getter
@Setter
@NoArgsConstructor
public class RsNode extends BaseBO implements MerkleTreeSnapshotAgent.MerkleDataNode {
    /**
     * rs id
     */
    private String rsId;

    /**
     * status
     */
    private RsNodeStatusEnum status;

    /**
     * desc
     */
    private String desc;

    @Override public String getUniqKey() {
        return rsId;
    }
}
