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
package bftsmart.statemanagement.strategy.durability;

import bftsmart.statemanagement.ApplicationState;
import bftsmart.tom.server.Recoverable;
import bftsmart.tom.server.defaultservices.durability.DurabilityCoordinator;
import bftsmart.tom.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The type State sender server.
 */
public class StateSenderServer implements Runnable {

    private ServerSocket server;
    private ApplicationState state;
    private Recoverable recoverable;
    private DurabilityCoordinator coordinator;
    private CSTRequest request;

    /**
     * Sets state.
     *
     * @param state the state
     */
    public void setState(ApplicationState state) {
        this.state = state;
    }

    /**
     * Sets recoverable.
     *
     * @param recoverable the recoverable
     */
    public void setRecoverable(Recoverable recoverable) {
        this.recoverable = recoverable;
        coordinator = (DurabilityCoordinator)(recoverable);
    }

    /**
     * Sets request.
     *
     * @param request the request
     */
    public void setRequest(CSTRequest request) {
        this.request = request;
    }

    /**
     * Instantiates a new State sender server.
     *
     * @param port the port
     */
    public StateSenderServer(int port) {
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Logger.printError(e.getMessage(), e);
        }
    }

    @Override public void run() {
        try {
            Socket socket = server.accept();
            StateSender sender = new StateSender(socket);
            state = coordinator.getState(request);
            sender.setState(state);
            new Thread(sender).start();
        } catch (IOException e) {
            Logger.printError(e.getMessage(), e);
        }
    }

}
