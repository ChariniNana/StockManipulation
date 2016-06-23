
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

public class StockWithMinMaxValue2 {
    private static int count;

    @Before
    public void init() {
        count = 0;
    }

    public static void main(String[] args) throws InterruptedException, SiddhiParserException {
        // Create Siddhi Manager
        SiddhiManager siddhiManager = new SiddhiManager();
        String tradesStream = "define stream trades ( Trade_ID string, TimeStamp long, Value double, SystemTime long); ";

        /*
         * String query1 = ("@info(name = 'query1') " +
         * "from trades#window.externalTime(TimeStamp, 3 min) " +
         * "select Trade_ID, TimeStamp, Value, max(Value) as Max_value, min(Value) as Min_value " +
         * "insert into minMax;" +
         * "@info(name = 'query2') "+
         * "from every e1=minMax, e2=minMax[Value > e1.Max_value or Value < e1.Min_value] " +
         * "select e2.Trade_ID, e2.TimeStamp, e2.Value as Current_value, ifThenElse(e2.Value > e1.Max_value,e1.Max_value,e1.Min_value) as PastMinOrMaxValue, ifThenElse(e2.Value > e1.Max_value,((e2.Value - e1.Max_value) * 100)/e1.Max_value,((e1.Min_value - e2.Value) * 100)/e1.Min_value) as Percentage, ifThenElse(e2.Value > e1.Max_value,'New Max','New Min') as NewMinOrMax "
         * +
         * "insert into export;");
         */
        String query1 = ("@info(name = 'query1') " + "from trades#window.externalTime(TimeStamp, 3 min) "
                + "select Trade_ID, TimeStamp, Value, max(Value) as Max_value, min(Value) as Min_value, SystemTime "
                + "insert into minMax;" + "@info(name = 'query2') "
                + "from every e1=minMax, e2=minMax[Max_value != e1.Max_value] "
                + "select e2.Trade_ID, e2.TimeStamp, e2.Value as Current_value, e1.Max_value as pastMax, e1.Min_value as pastMin, e2.SystemTime "
                + "insert into export;");

        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(tradesStream + query1);

        executionPlanRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                count++;
                // System.out.println(count);
            }
        });

        executionPlanRuntime.addCallback("export", new StreamCallback() {
            @Override
            public void receive(Event[] inEvents) {
                EventPrinter.print(inEvents);
                for (Event x:inEvents){
                    System.out.println("latency = "+ (System.currentTimeMillis()-(Long)(x.getData(5))));
                }
                //System.out.println(System.currentTimeMillis());
                // count++;
                // System.out.println(count);

            }
        });

        InputHandler inputHandler = executionPlanRuntime.getInputHandler("trades");

        executionPlanRuntime.start();

        Long myTimeStamp = 1466486960115L;
        System.out.println("Start: "+System.currentTimeMillis());
        inputHandler.send(new Object[] { "1.0", myTimeStamp, 1000D, System.currentTimeMillis() });

        /*
         * inputHandler.send(new Object[]{"1.0", 1466486960115L, 1000D});
         * inputHandler.send(new Object[]{"2.0", 1466486960116L, 99D});
         * inputHandler.send(new Object[]{"3.0", 1466486960117L, 100D});
         * inputHandler.send(new Object[]{"4.0", 1466486960118L, 2000D});
         */

        for (double j = 0; j < 10000000; j++) {

            if ((j + 1.0) % 50.0 == 0.0) {
                ++myTimeStamp;
                // ++myTimeStamp;
                // myTimeStamp+=1L;

            }
            inputHandler.send(new Object[] { Double.toString(j + 2D), myTimeStamp, 99D, System.currentTimeMillis() });
            /*if (j + 2D == 9000001.0){
                System.out.println(System.currentTimeMillis());
            }*/
            /*
             * if (j != 9001000){
             * inputHandler.send(new Object[]{Double.toString(j + 2D), myTimeStamp, 99D});
             * }
             * else{
             * inputHandler.send(new Object[]{Double.toString(j + 2D), 1466487140115L, 9999D});
             * }
             */
        }
        /*
         * System.out.println(count);
         * System.out.println(myTimeStamp);
         * System.out.println(System.currentTimeMillis());
         */

        Thread.sleep(100);

        executionPlanRuntime.shutdown();

    }
}
