package com.leidos.glidepath.dvi.simulated;

import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.dvi.domain.DviParameters;
import com.leidos.glidepath.dvi.domain.DviUIMessage;
import com.leidos.glidepath.dvi.services.DviExecutorService;
import com.leidos.glidepath.dvi.simulated.SimulatedUiMessagePeriodicExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SimulatedDviExecutorService {

    private static Logger logger = LoggerFactory.getLogger(SimulatedDviExecutorService.class);
    private static int refreshCounter = 0;
    private int uiRefresh = 10;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private SimpMessagingTemplate template;

    SimulatedUiMessagePeriodicExecutor periodicExecutor;

    private DviUIMessage dviUiMessage;


    public boolean start()   {
        boolean result = false;

        // set maximum speed from configuration
        DviParameters dviParameters = DviParameters.getInstance();
        dviParameters.setMaximumSpeed(appConfig.getMaximumSpeed());

        uiRefresh = appConfig.getUiRefresh();

        try   {
            periodicExecutor = new SimulatedUiMessagePeriodicExecutor(this, appConfig.getPeriodicDelay());

            result = periodicExecutor.initialize();
        }
        catch(Exception e)   {
            result = false;
            logger.error("Error starting Periodic Executor: ", e);
        }

        return result;
    }


    public void stop()   {
        periodicExecutor.stop();
    }

    protected void printConfig()   {
        logger.info(appConfig.toString());
    }


    public boolean getAutoStartConsumption()   {
        return appConfig.getAutoStartConsumption();
    }


    public synchronized void setDviUiMessage(DviUIMessage uiMessage)   {
        this.dviUiMessage = uiMessage;
        logger.info("Setting DomainObject from consumer: " + uiMessage.toString());
        if (refreshCounter >= uiRefresh - 1)   {
            template.convertAndSend("/topic/dvitopic", uiMessage);
            refreshCounter = 0;
        }
        else   {
            refreshCounter += 1;
        }
    }


}