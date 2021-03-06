/**
 * Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bftsmart.reconfiguration;

import bftsmart.reconfiguration.views.View;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * The type Reconfigure reply.
 *
 * @author eduardo
 */
public class ReconfigureReply implements Externalizable {

    private View newView;
    private String[] joinSet;

    //The ideal thing now would be identifying the consensus with a
    // tuple (view number, consensus number in increasing order)
    //Ex: (0,0),(0,1)... update to next view... (1,0),(1,1),....
    private int lastExecConsId = -1;

    private int execLeader = -1;

    /**
     * Instantiates a new Reconfigure reply.
     */
    public ReconfigureReply() {
    }

    /**
     * Instantiates a new Reconfigure reply.
     *
     * @param newView            the new view
     * @param joinSet            the join set
     * @param lastExectConsensus the last exect consensus
     * @param leader             the leader
     */
    public ReconfigureReply(View newView, String[] joinSet, int lastExectConsensus, int leader) {
        this.newView = newView;
        this.lastExecConsId = lastExectConsensus;
        this.execLeader = leader;

        if (joinSet == null) {
            this.joinSet = new String[0];
        } else {
            this.joinSet = joinSet;
        }
    }

    /**
     * Gets view.
     *
     * @return the view
     */
    public View getView() {
        return newView;
    }

    /**
     * Get join set string [ ].
     *
     * @return the string [ ]
     */
    public String[] getJoinSet() {
        return joinSet;

    }

    /**
     * Gets exec leader.
     *
     * @return the exec leader
     */
    public int getExecLeader() {
        return execLeader;
    }

    /**
     * Gets last exec cons id.
     *
     * @return the last exec cons id
     */
    public int getLastExecConsId() {
        return lastExecConsId;
    }

    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(newView);
        out.writeInt(this.lastExecConsId);
        out.writeInt(this.execLeader);

        out.writeInt(joinSet.length);

        for (int i = 0; i < joinSet.length; i++) {
            out.writeUTF(joinSet[i]);
        }
    }

    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        newView = (View)in.readObject();
        this.lastExecConsId = in.readInt();
        this.execLeader = in.readInt();
        joinSet = new String[in.readInt()];
        for (int i = 0; i < joinSet.length; i++) {
            joinSet[i] = in.readUTF();
        }
    }

}
