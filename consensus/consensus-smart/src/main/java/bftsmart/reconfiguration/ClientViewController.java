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

import java.net.InetSocketAddress;

/**
 * The type Client view controller.
 *
 * @author eduardo
 */
public class ClientViewController extends ViewController {

    /**
     * Instantiates a new Client view controller.
     *
     * @param procId the proc id
     */
    public ClientViewController(int procId) {
        super(procId);
        View cv = getViewStore().readView();
        if (cv == null) {
            reconfigureTo(new View(0, getStaticConf().getInitialView(), getStaticConf().getF(), getInitAdddresses()));
        } else {
            reconfigureTo(cv);
        }
    }

    /**
     * Instantiates a new Client view controller.
     *
     * @param procId     the proc id
     * @param configHome the config home
     */
    public ClientViewController(int procId, String configHome) {
        super(procId, configHome);
        View cv = getViewStore().readView();
        if (cv == null) {
            reconfigureTo(new View(0, getStaticConf().getInitialView(), getStaticConf().getF(), getInitAdddresses()));
        } else {
            reconfigureTo(cv);
        }
    }

    /**
     * Update current view from repository.
     */
    public void updateCurrentViewFromRepository() {
        this.currentView = getViewStore().readView();
    }

    private InetSocketAddress[] getInitAdddresses() {
        int nextV[] = getStaticConf().getInitialView();
        InetSocketAddress[] addresses = new InetSocketAddress[nextV.length];
        for (int i = 0; i < nextV.length; i++) {
            addresses[i] = getStaticConf().getRemoteAddress(nextV[i]);
        }

        return addresses;
    }
}