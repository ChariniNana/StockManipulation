
/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.log4j.Logger;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.siddhi.core.util.EventPrinter;
import org.wso2.siddhi.query.compiler.exception.SiddhiParserException;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

public class NewStockMinMaxValue {
    static final Logger log = Logger.getLogger(NewStockMinMaxValue.class);
    private static int count;
    private static long currentSystemTime;

    @Before
    public void init() {
        count = 0;
    }

    public static void main(String[] args) throws InterruptedException, SiddhiParserException {
        log.info("NewStockMinMaxValue class: For each incoming stock data , the system detects whether " +
                 "it's above or below the stock's previous highest/lowest value in most recent 3 minutes.  " +
                 "If so, an alert is given specifying the percentage increment/decrement of highest/lowest value.");

        // Create Siddhi Manager
        SiddhiManager siddhiManager = new SiddhiManager();
        String tradesStream = "define stream trades ( Trade_ID string, TimeStamp long, Value double, SystemTime long); ";

         String query1 = ("@info(name = 'query1') " +
         "from trades#window.externalTime(TimeStamp, 3 min) " +
         "select Trade_ID, TimeStamp, Value, max(Value) as Max_value, min(Value) as Min_value, SystemTime " +
         "insert into minMax;" +
         "" +
         "@info(name = 'query2') "+
         "from every e1=minMax, e2=minMax[Value > e1.Max_value or Value < e1.Min_value] " +
         "select e2.Trade_ID, e2.TimeStamp, e2.Value as Current_value, " +
                 "ifThenElse(e2.Value > e1.Max_value,e1.Max_value,e1.Min_value) as PastMinOrMaxValue, " +
                 "ifThenElse(e2.Value > e1.Max_value,((e2.Value - e1.Max_value) * 100)/e1.Max_value,((e1.Min_value - e2.Value) * 100)/e1.Min_value) as Percentage, " +
                 "ifThenElse(e2.Value > e1.Max_value,'New Max','New Min') as NewMinOrMax, " +
                 "e2.SystemTime "+
                 "insert into export;");


        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(tradesStream + query1);

        executionPlanRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                count++; // count the number of events processed
            }
        });

        executionPlanRuntime.addCallback("export", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events); // send alert for new highest/lowest value
                // milliseconds since the arrival of this event
                for (Event x : events) {
                    System.out.println("Latency in millisecond= " + (System.currentTimeMillis() - (Long) (x.getData(6))));
                }
                currentSystemTime = System.currentTimeMillis();
            }
        });

        InputHandler inputHandler = executionPlanRuntime.getInputHandler("trades");

        executionPlanRuntime.start();

        long myTimeStamp = 1466486960115L; // artificial time stamp used to ensure that 50k events are sent in 1 sec
        long startTime = System.currentTimeMillis();
        System.out.println("Process started at: " + startTime + " milliseconds");

        // first event is sent
        inputHandler.send(new Object[] { "1.0", myTimeStamp, 1000D, System.currentTimeMillis() });

        // send another 10000000 events with value 99
        for (double j = 0; j < 10000000; j++) {

            if ((j + 1.0) % 50.0 == 0.0) {
                ++myTimeStamp;
            }
            inputHandler.send(new Object[]{Double.toString(j + 2D), myTimeStamp, 99D, System.currentTimeMillis()});
        }

        // send another event with value 2000D
        inputHandler.send(new Object[]{"10000002.0", myTimeStamp, 2000D, System.currentTimeMillis()});

        System.out.println(count + " events processed in "+ (currentSystemTime-startTime)/1000 +" seconds");

        Thread.sleep(100);

        executionPlanRuntime.shutdown();

    }
}
