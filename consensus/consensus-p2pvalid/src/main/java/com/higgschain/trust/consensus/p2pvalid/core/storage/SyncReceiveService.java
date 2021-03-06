package com.higgschain.trust.consensus.p2pvalid.core.storage;

import com.higgschain.trust.consensus.p2pvalid.core.ResponseCommand;
import com.higgschain.trust.consensus.p2pvalid.core.ValidCommand;
import com.higgschain.trust.consensus.p2pvalid.core.ValidResponseWrap;
import com.higgschain.trust.consensus.p2pvalid.core.ValidSyncCommit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The type Sync receive service.
 */
@Slf4j @Component public class SyncReceiveService extends BaseReceiveService {

    @Override public ValidResponseWrap<? extends ResponseCommand> receive(ValidCommand validCommand) {
        try {
            Object object = validConsensus.getValidExecutor().execute(new ValidSyncCommit(validCommand));
            return ValidResponseWrap.successResponse(object);
        } catch (Exception e) {
            log.warn("execute valid command error:{}", e.getMessage());
            return ValidResponseWrap.failedResponse(e.getMessage());
        }
    }
}
