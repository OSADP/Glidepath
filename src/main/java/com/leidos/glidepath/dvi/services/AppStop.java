package com.leidos.glidepath.dvi.services;

import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * A Runnable class to stop the application
 *
 */
public class AppStop implements Runnable {

    @Override
    public void run() {
        ConfigurableApplicationContext springApp = (ConfigurableApplicationContext) GlidepathApplicationContext.getInstance().getApplicationContext();
        springApp.close();
        System.exit(0);
    }

}
