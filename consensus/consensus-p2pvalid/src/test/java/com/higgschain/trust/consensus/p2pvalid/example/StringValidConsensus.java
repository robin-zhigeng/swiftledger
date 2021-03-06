package com.higgschain.trust.consensus.p2pvalid.example;

import com.higgschain.trust.consensus.p2pvalid.core.P2PValidCommit;
import com.higgschain.trust.consensus.p2pvalid.core.ValidConsensus;
import com.higgschain.trust.consensus.p2pvalid.example.slave.ValidateCommand;
import org.springframework.stereotype.Component;

/**
 * The type String valid consensus.
 *
 * @author cwy
 */
@Component
public class StringValidConsensus extends ValidConsensus {

    /**
     * Test string valid.
     *
     * @param commit the commit
     */
    public void testStringValid(P2PValidCommit<StringValidCommand> commit) {
        System.out.println("command2 is " + commit.operation().get());
        commit.close();
    }

    /**
     * Test validate command.
     *
     * @param commit the commit
     */
    public void testValidateCommand(P2PValidCommit<ValidateCommand> commit) {
        System.out.println("ValidateCommand is " + commit.operation().get());
        commit.close();
    }
}
