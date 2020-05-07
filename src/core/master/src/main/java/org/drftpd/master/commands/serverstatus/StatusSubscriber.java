package org.drftpd.master.commands.serverstatus;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.drftpd.common.dynamicdata.Key;
import org.drftpd.common.dynamicdata.KeyedMap;
import org.drftpd.master.event.SlaveEvent;

public class StatusSubscriber {
    private static StatusSubscriber _subscriber = null;

    private StatusSubscriber() {
        // Subscribe to events
        AnnotationProcessor.process(this);
    }

    /**
     * Checks if this subscriber is already listening to events, otherwise, initialize it.
     */
    public static void checkSubscription() {
        if (_subscriber == null) {
            _subscriber = new StatusSubscriber();
        }
    }

    /**
     * Remove the reference to the current subscriber so that it can be GC'ed.
     */
    private static void nullify() {
        _subscriber = null;
    }

    @EventSubscriber
    public void onSlaveEvent(SlaveEvent event) {
        KeyedMap<Key<?>, Object> keyedMap = event.getRSlave().getTransientKeyedMap();
        if (event.getCommand().equals("ADDSLAVE")) {
            keyedMap.setObject(ServerStatus.CONNECTTIME, System.currentTimeMillis());
        } else if (event.getCommand().equals("DELSLAVE")) {
            keyedMap.remove(ServerStatus.CONNECTTIME);
        }
    }
}
