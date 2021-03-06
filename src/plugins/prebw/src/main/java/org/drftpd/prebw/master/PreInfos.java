package org.drftpd.prebw.master;

import java.util.LinkedList;

/**
 * @author lh
 */
public class PreInfos {
    private static PreInfos ref;
    private final LinkedList<PreInfo> _preInfos;

    private PreInfos() {
        _preInfos = new LinkedList<>();
    }

    public static synchronized PreInfos getPreInfosSingleton() {
        if (ref == null)
            // it's ok, we can call this constructor
            ref = new PreInfos();
        return ref;
    }

    public LinkedList<PreInfo> getPreInfos() {
        return _preInfos;
    }

    public void clearPreInfos() {
        _preInfos.clear();
    }
}
