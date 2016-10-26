package com.leidos.glidepath.dvi.simulated.testconsumers;

import com.leidos.glidepath.IConsumerInitializer;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;

/**
 * Skeleton XgvConsumerInitializer
 */
public class TestXgvConsumerInitializer  implements IConsumerInitializer {

    ILogger logger = LoggerManager.getLogger(TestXgvConsumerInitializer.class);

    @Override
    public Boolean call() throws Exception {
        logger.debug("CON", "Executing TestXvgConsumerInitializer call()");
        return new Boolean(true);
    }
}
