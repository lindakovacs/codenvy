/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */

package com.codenvy.analytics.pig.scripts;


import com.codenvy.analytics.BaseTest;
import com.codenvy.analytics.datamodel.LongValueData;
import com.codenvy.analytics.metrics.Context;
import com.codenvy.analytics.metrics.Metric;
import com.codenvy.analytics.metrics.Parameters;
import com.codenvy.analytics.metrics.ide_usage.AbstractTimeSpentInAction;
import com.codenvy.analytics.pig.scripts.util.Event;
import com.codenvy.analytics.pig.scripts.util.LogGenerator;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public class TestTimeSpentInAction extends BaseTest {

    private static final String COLLECTION = TestTimeSpentInAction.class.getSimpleName().toLowerCase();

    @BeforeClass
    public void prepare() throws Exception {
        List<Event> events = new ArrayList<>();

        // user1@gmail.com 6m session
        events.add(Event.Builder.createRunStartedEvent("user1@gmail.com", "ws1", "project", "type", "").withDate(
                "2013-01-01").withTime("19:00:00").build());
        events.add(Event.Builder.createRunFinishedEvent("user1@gmail.com", "ws1", "project", "type", "").withDate(
                "2013-01-01").withTime("19:06:00").build());

        // user2@gmail.com 2m session
        events.add(Event.Builder.createRunStartedEvent("user2@gmail.com", "ws2", "project", "type", "id1").withDate(
                "2013-01-01").withTime("19:08:00").build());
        events.add(Event.Builder.createRunFinishedEvent("user2@gmail.com", "ws2", "project", "type", "id1").withDate(
                "2013-01-01").withTime("19:10:00").build());

        // user1@gmail.com 1m session
        events.add(Event.Builder.createRunStartedEvent("user1@gmail.com", "ws1", "project", "type", "id2").withDate(
                "2013-01-01").withTime("19:11:00").build());
        events.add(Event.Builder.createRunFinishedEvent("user1@gmail.com", "ws1", "project", "type", "id2").withDate(
                "2013-01-01").withTime("19:12:00").build());

        // corrupted session events, 'run-started' event is absent
        events.add(Event.Builder.createRunFinishedEvent("user4@gmail.com", "ws1", "project", "type", "").withDate(
                "2013-01-01").withTime("19:13:00").build());

        // corrupted session events, 'run-finished' event is absent
        events.add(Event.Builder.createRunStartedEvent("user1@gmail.com", "ws1", "project", "type", "").withDate(
                "2013-01-01").withTime("19:07:00").build());

        File log = LogGenerator.generateLog(events);

        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20130101");
        builder.put(Parameters.TO_DATE, "20130101");
        builder.put(Parameters.USER, Parameters.USER_TYPES.REGISTERED.name());
        builder.put(Parameters.WS, Parameters.WS_TYPES.ANY.name());
        builder.put(Parameters.STORAGE_TABLE, COLLECTION);
        builder.put(Parameters.EVENT, "run");
        builder.put(Parameters.LOG, log.getAbsolutePath());
        pigServer.execute(ScriptType.TIME_SPENT_IN_ACTION, builder.build());
    }

    @Test
    public void testDateFilter() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20130101");
        builder.put(Parameters.TO_DATE, "20130101");

        Metric metric = new TestedAbstractTimeSpentInAction();
        Assert.assertEquals(metric.getValue(builder.build()), new LongValueData(540000));
    }

    @Test
    public void testWrongDateFilter() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20130102");
        builder.put(Parameters.TO_DATE, "20130102");

        Metric metric = new TestedAbstractTimeSpentInAction();
        Assert.assertEquals(metric.getValue(builder.build()), new LongValueData(0));
    }


    @Test
    public void testSingleUserFilter() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20130101");
        builder.put(Parameters.TO_DATE, "20130101");
        builder.put(Parameters.USER, "user1@gmail.com");

        Metric metric = new TestedAbstractTimeSpentInAction();
        Assert.assertEquals(metric.getValue(builder.build()), new LongValueData(420000));
    }

    @Test
    public void testDoubleUserFilter() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20130101");
        builder.put(Parameters.TO_DATE, "20130101");
        builder.put(Parameters.USER, "user1@gmail.com,user2@gmail.com");

        Metric metric = new TestedAbstractTimeSpentInAction();
        Assert.assertEquals(metric.getValue(builder.build()), new LongValueData(540000));
    }

    @Test
    public void testSeveralFilters() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20130101");
        builder.put(Parameters.TO_DATE, "20130101");
        builder.put(Parameters.USER, "user1@gmail.com,user2@gmail.com");
        builder.put(Parameters.WS, "ws2");

        Metric metric = new TestedAbstractTimeSpentInAction();
        Assert.assertEquals(metric.getValue(builder.build()), new LongValueData(120000));

    }

    //-------------------- Tested classes --------------------

    private class TestedAbstractTimeSpentInAction extends AbstractTimeSpentInAction {

        public TestedAbstractTimeSpentInAction() {
            super(COLLECTION);
        }

        @Override
        public String getStorageCollectionName() {
            return COLLECTION;
        }

        @Override
        public String getDescription() {
            return null;
        }
    }
}


