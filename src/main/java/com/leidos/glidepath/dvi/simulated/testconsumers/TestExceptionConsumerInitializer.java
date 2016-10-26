package com.leidos.glidepath.dvi.simulated.testconsumers;

import com.leidos.glidepath.IConsumerInitializer;

/**
 * An initializer that throws an exception
 */
public class TestExceptionConsumerInitializer  implements IConsumerInitializer {
    @Override
    public Boolean call() throws Exception {
        throw new Exception("Iniitializer throwing Exception.");
    }
}
