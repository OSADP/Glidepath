package com.leidos.glidepath.dvi;

import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.services.DviExecutorService;
import com.leidos.glidepath.dvi.simulated.SimulatedDviExecutorService;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;
import com.leidos.glidepath.appcommon.Constants;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import java.io.IOException;

@ComponentScan
@EnableAutoConfiguration
public class SpeedControl {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SpeedControl.class, args);

        // set our context for non spring managed classes
        GlidepathApplicationContext.getInstance().setApplicationContext(context);
        AppConfig config = GlidepathApplicationContext.getInstance().getAppConfig();

        //SimulatedDviExecutorService service = context.getBean(SimulatedDviExecutorService.class);
        DviExecutorService service = context.getBean(DviExecutorService.class);

        LoggerManager.setOutputFile(config.getProperty("log.path") + "/" + Constants.LOG_FILE);
        LoggerManager.setRealTimeOutput(config.getLogRealTimeOutput());
        ILogger logger = LoggerManager.getLogger(SpeedControl.class);

        logger.infof("TAG", "####### SpeedControl started ########");
        try   {
            LoggerManager.writeToDisk();
        }
        catch(IOException ioe)   {
            System.err.println("Error writing log to disk: " + ioe.getMessage());
        }

        if (service.getAutoStartConsumption())   {
            service.start();
        }

    }
}