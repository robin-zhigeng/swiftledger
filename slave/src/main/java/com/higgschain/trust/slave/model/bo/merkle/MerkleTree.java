package com.higgschain.trust.slave.model.bo.merkle;

import com.higgschain.trust.slave.api.enums.MerkleTypeEnum;
import com.higgschain.trust.slave.model.bo.BaseBO;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * The type Merkle tree.
 *
 * @author WangQuanzhou
 * @desc merkle tree BO
 * @date 2018 /3/29 15:19
 */
@Getter @Setter public class MerkleTree extends BaseBO {
    // Merkle Root
    private String rootHash;

    // the total level of the merkle tree
    private int totalLevel;

    // the max index of leaf level of the merkle tree
    private Long maxIndex;

    // type of merkle tree，can be ACCOUNT or TX or CONTRACT
    private MerkleTypeEnum treeType;

    // merkle node map
    private Map<String, MerkleNode> nodeMap;

    /**
     * Instantiates a new Merkle tree.
     */
    public MerkleTree() {
    }

    /**
     * Instantiates a new Merkle tree.
     *
     * @param treeType the tree type
     */
    public MerkleTree(MerkleTypeEnum treeType) {
        this.treeType = treeType;
    }

    /**
     * Instantiates a new Merkle tree.
     *
     * @param rootHash   the root hash
     * @param totalLevel the total level
     * @param maxIndex   the max index
     * @param treeType   the tree type
     * @param nodeMap    the node map
     */
    public MerkleTree(String rootHash, int totalLevel, Long maxIndex, MerkleTypeEnum treeType,
        Map<String, MerkleNode> nodeMap) {
        this.rootHash = rootHash;
        this.totalLevel = totalLevel;
        this.maxIndex = maxIndex;
        this.treeType = treeType;
        this.nodeMap = nodeMap;
    }
}
