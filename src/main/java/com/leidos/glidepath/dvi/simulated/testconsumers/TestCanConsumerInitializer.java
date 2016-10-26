package com.leidos.glidepath.dvi.simulated.testconsumers;

import com.leidos.glidepath.IConsumerInitializer;

/**
 * Skeleton CanConsumerInitializer
 */
public class TestCanConsumerInitializer  implements IConsumerInitializer {
    @Override
    public Boolean call() throws Exception {
        return new Boolean(true);
    }
}
