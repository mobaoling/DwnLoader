package com.sf.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created on 15/11/5.
 * Function:
 */
public class ThreadUtil {

    public static ExecutorService mFixThreads;

    public static ExecutorService getCachedThreadPool(){
        if (null == mFixThreads) {
            mFixThreads = Executors.newCachedThreadPool();
        }
        return mFixThreads;
    }
}
