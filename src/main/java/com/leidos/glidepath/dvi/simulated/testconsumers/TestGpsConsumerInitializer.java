package com.leidos.glidepath.dvi.simulated.testconsumers;

import com.leidos.glidepath.IConsumerInitializer;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;

/**
 * Skeleton GpsConsumerInitializer
 */
public class TestGpsConsumerInitializer  implements IConsumerInitializer {

    ILogger logger = LoggerManager.getLogger(TestGpsConsumerInitializer.class);

    @Override
    public Boolean call() throws Exception {
        logger.debug("CON", "Executing TestGpsConsumerInitializer call()");
        return new Boolean(true);
    }
}
