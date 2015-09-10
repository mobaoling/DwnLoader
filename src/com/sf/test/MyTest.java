package com.sf.test;

import android.test.InstrumentationTestCase;

import com.sf.dwnload.DwnManager;

import java.io.IOException;

/**
 * Created by caojianbo on 15/8/26.
 */
public class MyTest extends InstrumentationTestCase {

    public void testDwnload() throws Exception {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                DwnManager dwnManager = new DwnManager(getInstrumentation().getContext(), 2);
//                assertEquals(false, dwnManager.dwnFile(null, null, null));
            }
        });

    }
}
