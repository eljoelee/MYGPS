package com.gps.trs_user.mygps;

import android.app.Application;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ApplicationTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest extends ApplicationTestCase<Application> {
    public ExampleInstrumentedTest() {
        super(Application.class);
    }
    public void example()
    {
        assertTrue(true);
    }
    public void test1()
    {
        assertTrue(false);
    }
}
