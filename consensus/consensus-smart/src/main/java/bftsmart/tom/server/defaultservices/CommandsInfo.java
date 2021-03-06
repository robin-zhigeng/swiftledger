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
package bftsmart.tom.server.defaultservices;

import bftsmart.tom.MessageContext;

import java.io.Serializable;
import java.util.Arrays;

/**
 * The type Commands info.
 *
 * @author Joao Sousa
 */
public class CommandsInfo implements Serializable {

    private static final long serialVersionUID = 342711292879899682L;

    /**
     * The Commands.
     */
    public byte[][] commands;
    /**
     * The Msg ctx.
     */
    public MessageContext[] msgCtx;

    /**
     * Instantiates a new Commands info.
     */
    public CommandsInfo() {
        this.commands = null;
        this.msgCtx = null;
    }

    /**
     * Instantiates a new Commands info.
     *
     * @param commands the commands
     * @param msgCtx   the msg ctx
     */
    public CommandsInfo(byte[][] commands, MessageContext[] msgCtx) {
        this.commands = commands;
        MessageContext[] onlyNeeded = null;
        if (msgCtx != null && msgCtx.length > 0) {
            onlyNeeded = new MessageContext[msgCtx.length];
            for (int i = 0; i < msgCtx.length; i++) {
                MessageContext msg =
                    new MessageContext(msgCtx[i].getSender(), msgCtx[i].getViewID(), msgCtx[i].getType(),
                        msgCtx[i].getSession(), msgCtx[i].getSequence(), msgCtx[i].getOperationId(),
                        msgCtx[i].getReplyServer(), msgCtx[i].getSignature(), msgCtx[i].getTimestamp(),
                        msgCtx[i].getNumOfNonces(), msgCtx[i].getSeed(), msgCtx[i].getRegency(), msgCtx[i].getLeader(),
                        msgCtx[i].getConsensusId(), msgCtx[i].getProof(), msgCtx[i].getFirstInBatch(),
                        msgCtx[i].isNoOp());
                onlyNeeded[i] = msg;
            }
        }
        this.msgCtx = onlyNeeded;
    }

    @Override public boolean equals(Object obj) {
        if (obj instanceof CommandsInfo) {
            CommandsInfo ci = (CommandsInfo)obj;

            if ((this.commands != null && ci.commands == null) || (this.commands == null && ci.commands != null)) {
                //Logger.println(("[CommandsInfo] returing FALSE!1");
                return false;
            }

            if (this.commands != null && ci.commands != null) {

                if (this.commands.length != ci.commands.length) {
                    //Logger.println(("[CommandsInfo] returing FALSE!2");
                    return false;
                }

                for (int i = 0; i < this.commands.length; i++) {

                    if (this.commands[i] == null && ci.commands[i] != null) {
                        //Logger.println(("[CommandsInfo] returing FALSE!3");
                        return false;
                    }

                    if (this.commands[i] != null && ci.commands[i] == null) {
                        //Logger.println(("[CommandsInfo] returing FALSE!4");
                        return false;
                    }

                    if (!(this.commands[i] == null && ci.commands[i] == null) && (!Arrays
                        .equals(this.commands[i], ci.commands[i]))) {
                        //Logger.println(("[CommandsInfo] returing FALSE!5" + (this.commands[i] == null) + " " + (ci.commands[i] == null));
                        return false;
                    }
                }
            }
            //Logger.println(("[CommandsInfo] returnig........");
            //Logger.println(((this.epoch == ci.epoch) + " " + (this.leader == ci.leader));
            return true;
        }
        //Logger.println(("[CommandsInfo] returing FALSE!");
        return false;
    }

    @Override public int hashCode() {

        int hash = 1;

        if (this.commands != null) {
            for (int i = 0; i < this.commands.length; i++) {
                if (this.commands[i] != null) {
                    for (int j = 0; j < this.commands[i].length; j++)
                        hash = hash * 31 + (int)this.commands[i][j];
                } else {
                    hash = hash * 31 + 0;
                }
            }
        } else {
            hash = hash * 31 + 0;
        }

        return hash;
    }

    //These methods were used when the class extended interface Externizable,
    //but this was causing serialization problems when the batch of operations
    //had very large commands. Using the Serializable interface, the issue was solved
    /*@Override
    public void writeExternal(ObjectOutput out) throws IOException {
        
        if (commands != null) {
            out.writeInt(commands.length);
            
            for (byte[] m : commands) {
                out.writeInt(m.length);
                out.write(m);
            }
        }
        else {
            out.writeInt(0);
        }
        
        if (msgCtx != null) {
            out.writeInt(msgCtx.length);
            
            for (MessageContext m : msgCtx)
                out.writeObject(m);
            
        }
        else {
            out.writeInt(0);
        }        

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        int size = in.readInt();
        if (size > 0) {
            commands = new byte[size][];
            
            for (int i = 0; i < commands.length; i++) {
                
                int length = in.readInt();
                commands[i] = new byte[length];
                in.read(commands[i]);
            }
        }
        
        size = in.readInt();
        if (size > 0) {
            
            msgCtx = new MessageContext[size];
            for (int i = 0; i < commands.length; i++)
                msgCtx[i] = (MessageContext) in.readObject();
            
        }
    }*/
}
