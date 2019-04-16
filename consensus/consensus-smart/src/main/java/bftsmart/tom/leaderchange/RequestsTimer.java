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
package bftsmart.tom.leaderchange;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.Logger;
import bftsmart.tom.util.TOMUtil;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * This thread serves as a manager for all timers of pending requests.
 */
public class RequestsTimer {

    private Timer timer = new Timer("request timer");
    private RequestTimerTask rtTask = null;
    private TOMLayer tomLayer; // TOM layer
    private long timeout;
    private long shortTimeout;
    private TreeSet<TOMMessage> watched = new TreeSet<TOMMessage>();
    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private boolean enabled = true;

    private ServerCommunicationSystem communication; // Communication system between replicas
    private ServerViewController controller; // Reconfiguration manager

    private Hashtable<Integer, Timer> stopTimers = new Hashtable<>();

    //private Storage st1 = new Storage(100000);
    //private Storage st2 = new Storage(10000);

    /**
     * Creates a new instance of RequestsTimer
     *
     * @param tomLayer      TOM layer
     * @param communication the communication
     * @param controller    the controller
     */
    public RequestsTimer(TOMLayer tomLayer, ServerCommunicationSystem communication, ServerViewController controller) {
        this.tomLayer = tomLayer;

        this.communication = communication;
        this.controller = controller;

        this.timeout = this.controller.getStaticConf().getRequestTimeout();
        this.shortTimeout = -1;
    }

    /**
     * Sets short timeout.
     *
     * @param shortTimeout the short timeout
     */
    public void setShortTimeout(long shortTimeout) {
        this.shortTimeout = shortTimeout;
    }

    /**
     * Sets timeout.
     *
     * @param timeout the timeout
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Gets timeout.
     *
     * @return the timeout
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Start timer.
     */
    public void startTimer() {
        if (rtTask == null) {
            long t = (shortTimeout > -1 ? shortTimeout : timeout);
            //shortTimeout = -1;
            rtTask = new RequestTimerTask();
            if (controller.getCurrentViewN() > 1)
                timer.schedule(rtTask, t);
        }
    }

    /**
     * Stop timer.
     */
    public void stopTimer() {
        if (rtTask != null) {
            rtTask.cancel();
            rtTask = null;
        }
    }

    /**
     * Enabled.
     *
     * @param phase the phase
     */
    public void Enabled(boolean phase) {

        enabled = phase;
    }

    /**
     * Is enabled boolean.
     *
     * @return the boolean
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Creates a timer for the given request
     *
     * @param request Request to which the timer is being createf for
     */
    public void watch(TOMMessage request) {
        //long startInstant = System.nanoTime();
        rwLock.writeLock().lock();
        watched.add(request);
        if (watched.size() >= 1 && enabled)
            startTimer();
        rwLock.writeLock().unlock();
    }

    /**
     * Cancels a timer for a given request
     *
     * @param request Request whose timer is to be canceled
     */
    public void unwatch(TOMMessage request) {
        //long startInstant = System.nanoTime();
        rwLock.writeLock().lock();
        if (watched.remove(request) && watched.isEmpty())
            stopTimer();
        rwLock.writeLock().unlock();
    }

    /**
     * Cancels all timers for all messages
     */
    public void clearAll() {
        TOMMessage[] requests = new TOMMessage[watched.size()];
        rwLock.writeLock().lock();

        watched.toArray(requests);

        for (TOMMessage request : requests) {
            if (request != null && watched.remove(request) && watched.isEmpty() && rtTask != null) {
                rtTask.cancel();
                rtTask = null;
            }
        }
        rwLock.writeLock().unlock();
    }

    /**
     * Run lc protocol.
     */
    public void run_lc_protocol() {

        long t = (shortTimeout > -1 ? shortTimeout : timeout);

        //Logger.println(("(RequestTimerTask.run) I SOULD NEVER RUN WHEN THERE IS NO TIMEOUT");

        LinkedList<TOMMessage> pendingRequests = new LinkedList<TOMMessage>();

        rwLock.readLock().lock();

        for (Iterator<TOMMessage> i = watched.iterator(); i.hasNext(); ) {
            TOMMessage request = i.next();
            if ((request.receptionTime + System.currentTimeMillis()) > t) {
                pendingRequests.add(request);
            } else {
                break;
            }
        }

        rwLock.readLock().unlock();

        if (!pendingRequests.isEmpty()) {
            for (ListIterator<TOMMessage> li = pendingRequests.listIterator(); li.hasNext(); ) {
                TOMMessage request = li.next();
                if (!request.timeout) {

                    request.signed = request.serializedMessageSignature != null;
                    tomLayer.forwardRequestToLeader(request);
                    request.timeout = true;
                    li.remove();
                }
            }

            if (!pendingRequests.isEmpty()) {
                Logger.println("Timeout for messages: " + pendingRequests);
                //Logger.debug = true;
                //tomLayer.requestTimeout(pendingRequests);
                //if (reconfManager.getStaticConf().getProcessId() == 4) Logger.debug = true;
                tomLayer.getSynchronizer().triggerTimeout(pendingRequests);
            } else {
                rtTask = new RequestTimerTask();
                timer.schedule(rtTask, t);
            }
        } else {
            rtTask = null;
            timer.purge();
        }

    }

    /**
     * Sets stop.
     *
     * @param regency the regency
     * @param stop    the stop
     */
    public void setSTOP(int regency, LCMessage stop) {

        stopSTOP(regency);

        SendStopTask stopTask = new SendStopTask(stop);
        Timer stopTimer = new Timer("Stop message");

        stopTimer.schedule(stopTask, timeout);

        stopTimers.put(regency, stopTimer);

    }

    /**
     * Stop all sto ps.
     */
    public void stopAllSTOPs() {
        Iterator stops = getTimers().iterator();
        while (stops.hasNext()) {
            stopSTOP((Integer)stops.next());
        }
    }

    /**
     * Stop stop.
     *
     * @param regency the regency
     */
    public void stopSTOP(int regency) {

        Timer stopTimer = stopTimers.remove(regency);
        if (stopTimer != null)
            stopTimer.cancel();

    }

    /**
     * Gets timers.
     *
     * @return the timers
     */
    public Set<Integer> getTimers() {

        return ((Hashtable<Integer, Timer>)stopTimers.clone()).keySet();

    }

    /**
     * Shutdown.
     */
    public void shutdown() {
        timer.cancel();
        stopAllSTOPs();
        java.util.logging.Logger.getLogger(RequestsTimer.class.getName()).log(Level.INFO, "RequestsTimer stopped.");

    }

    /**
     * The type Request timer task.
     */
    class RequestTimerTask extends TimerTask {

        @Override
        /**
         * This is the code for the TimerTask. It executes the timeout for the first
         * message on the watched list.
         */ public void run() {

            int[] myself = new int[1];
            myself[0] = controller.getStaticConf().getProcessId();

            communication.send(myself, new LCMessage(-1, TOMUtil.TRIGGER_LC_LOCALLY, -1, null));

        }
    }

    /**
     * The type Send stop task.
     */
    class SendStopTask extends TimerTask {

        private LCMessage stop;

        /**
         * Instantiates a new Send stop task.
         *
         * @param stop the stop
         */
        public SendStopTask(LCMessage stop) {
            this.stop = stop;
        }

        @Override
        /**
         * This is the code for the TimerTask. It sends a STOP
         * message to the other replicas
         */ public void run() {

            Logger.println("(SendStopTask.run) Re-transmitting STOP message to install regency " + stop.getReg());
            communication.send(controller.getCurrentViewOtherAcceptors(), this.stop);

            setSTOP(stop.getReg(), stop); //repeat
        }

    }
}
