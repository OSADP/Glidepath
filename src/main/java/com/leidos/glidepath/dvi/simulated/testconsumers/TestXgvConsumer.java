package com.leidos.glidepath.dvi.simulated.testconsumers;

import com.leidos.glidepath.IConsumerInitializer;
import com.leidos.glidepath.IConsumerTask;
import com.leidos.glidepath.IProducerTask;
import com.leidos.glidepath.appcommon.DataElementHolder;
import com.leidos.glidepath.appcommon.DataElementKey;
import com.leidos.glidepath.appcommon.DoubleDataElement;
import com.leidos.glidepath.appcommon.IntDataElement;
import com.leidos.glidepath.appcommon.utils.ConversionUtils;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;
import com.leidos.glidepath.dvi.simulated.testutils.XgvUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.IOException;

/**
 * Skeleton XgvConsumer
 */
public class TestXgvConsumer implements IConsumerTask, IProducerTask {

    ILogger logger = LoggerManager.getLogger(TestXgvConsumer.class);
    private static int counter = 0;
    private IConsumerInitializer initializer = new TestXgvConsumerInitializer();
    // line below used to debug failed initializer
    //private IConsumerInitializer initializer = new TestFailedConsumerInitializer();

    public TestXgvConsumer()   {
        // left blank
    }

    // provide flexibility to init with a failing initializer
    public TestXgvConsumer(IConsumerInitializer initializer)   {
        this.initializer = initializer;
    }

    @Override
    public boolean initialize() {
        logger.debug("CON", "Initializing TestXgvConsumer");
        return true;
    }

    @Override
    public void terminate() {
        logger.debug("CON", "Terminating TestXgvConsumer");
    }

    public IConsumerInitializer getInitializer()   {
        return initializer;
    }

    public DataElementHolder call() throws IOException {
        DateTime startTime = new DateTime();

        DataElementHolder holder = XgvUtils.createXgvStatus();

        // 480 is when light turns red, simulate stopping
        if (counter < 480)   {
            DataElementHolder velocityHolder = ConversionUtils.getInstance().getRandomSpeed(holder);
            holder.putAll(velocityHolder);
        }
        else   {
            holder.put(DataElementKey.SPEED, new DoubleDataElement(0.0));
        }

        Duration duration = new Duration(startTime, new DateTime());
        holder.put(DataElementKey.CYCLE_XGV, new IntDataElement((int) duration.getMillis()));

        counter += 1;

        return holder;
    }

    @Override
    public DataElementHolder produce(DataElementHolder holder)   {
        return new DataElementHolder();
    }
}