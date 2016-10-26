package com.leidos.glidepath.dvi.simulated.testconsumers;

import com.leidos.glidepath.IConsumerInitializer;

/**
 * An initializer that returns false
 */
public class TestFailedConsumerInitializer  implements IConsumerInitializer {
    @Override
    public Boolean call() throws Exception {
        return new Boolean(false);
    }
}
