package com.leidos.glidepath.dvi.simulated.testconsumers;

import com.leidos.glidepath.IConsumerInitializer;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;

/**
 * Skeleton CanConsumerInitializer
 */
public class TestAsdConsumerInitializer  implements IConsumerInitializer {

    private ILogger logger = LoggerManager.getLogger(TestAsdConsumerInitializer.class);

    @Override
    public Boolean call() throws Exception {
        logger.debug("CON", "Executing TestAsdConsumerInitializer call()");
        return new Boolean(true);
    }
}
