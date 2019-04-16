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

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The type View.
 *
 * @author eduardo
 */
public class View implements Serializable {

    private static final long serialVersionUID = 1466870385442069307L;

    private int id;
    private int f;
    private int[] processes;
    private Map<Integer, InetSocketAddress> addresses;

    /**
     * Instantiates a new View.
     *
     * @param id        the id
     * @param processes the processes
     * @param f         the f
     * @param addresses the addresses
     */
    public View(int id, int[] processes, int f, InetSocketAddress[] addresses) {
        this.id = id;
        this.processes = processes;
        this.addresses = new HashMap<Integer, InetSocketAddress>();

        for (int i = 0; i < this.processes.length; i++)
            this.addresses.put(processes[i], addresses[i]);
        Arrays.sort(this.processes);
        this.f = f;
    }

    /**
     * Is member boolean.
     *
     * @param id the id
     * @return the boolean
     */
    public boolean isMember(int id) {
        for (int i = 0; i < this.processes.length; i++) {
            if (this.processes[i] == id) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets pos.
     *
     * @param id the id
     * @return the pos
     */
    public int getPos(int id) {
        for (int i = 0; i < this.processes.length; i++) {
            if (this.processes[i] == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets id.
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Gets f.
     *
     * @return the f
     */
    public int getF() {
        return f;
    }

    /**
     * Gets n.
     *
     * @return the n
     */
    public int getN() {
        return this.processes.length;
    }

    /**
     * Get processes int [ ].
     *
     * @return the int [ ]
     */
    public int[] getProcesses() {
        return processes;
    }

    @Override public String toString() {
        String ret = "ID:" + id + "; F:" + f + "; Processes:";
        for (int i = 0; i < processes.length; i++) {
            ret = ret + processes[i] + "(" + addresses.get(processes[i]) + "),";
        }

        return ret;
    }

    /**
     * Gets address.
     *
     * @param id the id
     * @return the address
     */
    public InetSocketAddress getAddress(int id) {
        return addresses.get(id);
    }

}
