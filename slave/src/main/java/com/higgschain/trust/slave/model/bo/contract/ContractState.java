package com.higgschain.trust.slave.model.bo.contract;

import com.higgschain.trust.slave.core.service.snapshot.agent.MerkleTreeSnapshotAgent;
import com.higgschain.trust.slave.model.bo.BaseBO;
import lombok.Getter;
import lombok.Setter;

/**
 * The type Contract state.
 *
 * @author duhongming
 * @date 2018 /6/12
 */
@Getter
@Setter
public class ContractState extends BaseBO implements MerkleTreeSnapshotAgent.MerkleDataNode{
    private String address;
    private Object state;
    private String keyDesc;

    @Override public String getUniqKey() {
        return address;
    }
}
