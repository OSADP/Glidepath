package com.leidos.glidepath.dvi.services;

import com.leidos.glidepath.IConsumerTask;
import com.leidos.glidepath.appcommon.*;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.dvi.domain.*;

import com.leidos.glidepath.ead.TrajectoryFactory;
import com.leidos.glidepath.ead.ITrajectory;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LogEntry;
import com.leidos.glidepath.logger.LoggerManager;
import com.leidos.glidepath.xgv.XgvStatus;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The executor service that manages the device read cycles
 *
 */
@Service
public class DviExecutorService implements Callable<Boolean>, DisposableBean {

    private static ILogger logger = LoggerManager.getLogger(DviExecutorService.class);

    private AtomicBoolean bShutdown = new AtomicBoolean(true);
    private static int refreshCounter = 0;
    private int uiRefresh = 10;

    private AtomicBoolean bRecordData = new AtomicBoolean(true);

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private SimpMessagingTemplate template;

    private List<IConsumerTask> consumers;

    private ExecutorService executor;

    private PeriodicExecutor periodicExecutor;

    // Used to stop app after rolling logs
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    private DviUIMessage dviUiMessage;

    private ITrajectory trajectory;

    private int readCycle;

    /**
     * Adjustment to tweak sleep time at end of cycle, defaults to 5ms, but uses sleep.adjustment if configured
     */
    private int sleepAdjustment = 0;

    /**
     * Starts the device consumer threads
     *
     * @return
     */
    public boolean start()   {
        setLogLevel();

        boolean result = false;

        bShutdown = new AtomicBoolean(false);
        LoggerManager.setRecordData(true);

        // set maximum speed from configuration
        DviParameters dviParameters = DviParameters.getInstance();
        dviParameters.setMaximumSpeed(appConfig.getMaximumSpeed());

        readCycle = appConfig.getPeriodicDelay();
        uiRefresh = appConfig.getUiRefresh();
        sleepAdjustment = appConfig.getDefaultIntValue("sleep.adjustment", sleepAdjustment);

        try   {
            String trajectoryClassName = appConfig.getProperty("ead.trajectoryclass");
            trajectoryClassName = (trajectoryClassName == null) ? "com.leidos.glidepath.ead.SimulatedTrajectory" : trajectoryClassName;

            trajectory = TrajectoryFactory.newInstance(trajectoryClassName);
        }
        catch(Exception e)   {
            logger.error(ILogger.TAG_EXECUTOR, "Error loading simulated trajectory: " + e.getMessage());
            return false;
        }

        executor = Executors.newFixedThreadPool(2);

        try   {
            // allow setting any consumers to better support testing
            if (consumers == null || consumers.isEmpty())   {
                consumers = ConsumerFactory.getConsumers();
            }

            boolean bStateChange = GlidepathStateModel.getInstance().changeState(GlidepathState.SETUP, null);

            DeviceInitializer deviceInitializer = new DeviceInitializer(consumers);
            result = deviceInitializer.start();
            if (result)   {
                logger.output(ILogger.TAG_EXECUTOR, DataElementHolder.getLogHeader());

                bStateChange = GlidepathStateModel.getInstance().changeState(GlidepathState.STANDBY, null);

                periodicExecutor = new PeriodicExecutor(trajectory);
                periodicExecutor.addConsumers(consumers);
                executor.submit(this);
            }
            else   {
                // a consumer failed to initialize, stop the app
                try   {
                    LoggerManager.writeToDisk();
                }
                catch(Exception e)   {
                    System.err.println("Error writing to log file:" + e.getMessage());
                }

                autoRollLogs();
            }
        }
        catch(Exception e)   {
            result = false;
            logger.error(ILogger.TAG_EXECUTOR, "Error starting DeviceInitializer: " + e.getMessage());
        }

        return result;
    }

    /**
     * The read cycle loop
     *
     * Uses the periodicExecutor to submit each device read.  The response from the periodic executor
     * is what needs to be provided to the DVI clients
     *
     * @return
     */
    public Boolean call()   {
        Boolean result = new Boolean(false);
        DataElementHolder lastHolder = null;

        while (!bShutdown.get())   {
            logger.debug(ILogger.TAG_EXECUTOR, "");
            logger.debug("CYCLE", "****#### Start of Consumer Cycle ####****");

            DateTime startTime = new DateTime();

            DataElementHolder holder = new DataElementHolder();

            Future<DataElementHolder> future = executor.submit(periodicExecutor);
            try   {
                holder = future.get();

                // save current holder for motion status next read cycle
                lastHolder = holder;

                // only record consumer data result if flag is set
                if (bRecordData.get())   {
                    //logger.info("", "###### holder size: " + holder.size() + " ******");
                    logger.output(ILogger.TAG_EXECUTOR, holder.getLogString());
                    outputXgvStatus(holder);                }

                // ensure we have a fully loaded holder
                boolean debugMode = GlidepathApplicationContext.getInstance().getAppConfig().getBooleanValue("debug");

                // provide the dvi ui message IF we have debug set OR we have a fully loaded holder
                if (debugMode || holder.validate() )   {
                    DviUIMessage dviUiMessage = new DviUIMessage(holder);
                    setDviUiMessage(dviUiMessage);
                }
            }
            catch(Exception e)   {
                // error
                logger.error(ILogger.TAG_EXECUTOR, "Exception getting periodicExecutor future: " + e.getMessage());
                e.printStackTrace();
            }

            DateTime endTime = new DateTime();
            Duration dur = new Duration(startTime, endTime);
            long cycleLasted = dur.getMillis();

            if (bRecordData.get())   {
                logger.debug(ILogger.TAG_EXECUTOR, "Full consumer read cycle in Millis: " + cycleLasted);
            }

            // provide configuration capability to adjust sleep time to help obtain closer readCycles exactly equal to readCycle
            if (cycleLasted <= readCycle - sleepAdjustment)   {
                try   {
                    Thread.sleep(readCycle - cycleLasted - sleepAdjustment);
                }
                catch(InterruptedException ie)   {
                }
            }
            else   {
                if (bRecordData.get())   {
                    logger.warn(ILogger.TAG_EXECUTOR, "#### Read cycle exceeded periodicDelay ####: " + cycleLasted);
                }
            }

            try   { LoggerManager.writeToDisk(); }
            catch(Exception e)   {}

        }


        for (IConsumerTask consumer : consumers)   {
            consumer.terminate();
        }

        // when we close the app, we are mostly likely last in ENGAGED state
        //   setting back to state1 STARTUP for Apache purposes
        GlidepathStateModel.getInstance().changeState(GlidepathState.STARTUP, null);

        periodicExecutor.stop();
        trajectory.close();
        executor.shutdown();

        try   {
            LoggerManager.writeToDisk();
        }
        catch(Exception e)   {
            System.err.println("Error writing to log file:" + e.getMessage());
        }

        return new Boolean(true);
    }

    @Override
    public void destroy()   {
        stop();

        try   {
            Thread.sleep(100);
        }
        catch(Exception e) {};
        logger.info(ILogger.TAG_EXECUTOR,  "Destroying bean DviExecutorService via lifecycle destroy().");
    }

    /**
     * Signal to exit the read cycle loop
     */
    public void stop()   {
        bShutdown.getAndSet(true);
    }

    protected void printConfig()   {
        logger.info("", appConfig.toString());
    }

    /**
     * Configuration to indicate whether we should start reading devices on startup
     *
     * @return boolean indicating whether to start threads on startup
     */
    public boolean getAutoStartConsumption()   {
        return appConfig.getAutoStartConsumption();
    }

    /**
     * Provide direct setting of consumer list
     *
     * @param consumers
     */
    public void setConsumers(List<IConsumerTask> consumers)   {
        this.consumers = consumers;
    }


    /**
     * Sends message to clients subscribing to our stomp/websockets endpoints
     *
     * @param uiMessage
     */
    public synchronized void setDviUiMessage(DviUIMessage uiMessage)   {
        this.dviUiMessage = uiMessage;
        //logger.info(ILogger.TAG_EXECUTOR,  "Setting DomainObject from consumer: " + uiMessage.toString());
        if (refreshCounter >= uiRefresh - 1)   {
            template.convertAndSend("/topic/dvitopic", uiMessage);
            refreshCounter = 0;
        }
        else   {
            refreshCounter += 1;
        }
    }

    /**
     * Retrieve the state of the record data flag
     * @return
     */
    public boolean getRecordData()   {
        return bRecordData.get();
    }

    /**
     * Set whether we want consumer data logging to begin or stop
     *
     * @param flag
     */
    public void setRecordData(boolean flag)   {
        bRecordData.getAndSet(flag);
        LoggerManager.setRecordData(flag);
    }

    /**
     * Log XGV status members as tab delimited to ease debugging
     *
     * @param holder
     */
    private void outputXgvStatus(DataElementHolder holder)   {

        if (holder != null)   {
            XgvStatus xgvStatus = holder.getXgvStatus();

            if (xgvStatus != null)   {
                String xgvMessage = xgvStatus.toString().replaceAll(",", "\t");
                logger.debug("XSF", xgvMessage);
            }
        }
    }



    /**
     * Rename log files to start new recording
     *
     * @return boolean
     */
    public boolean moveLogs()   {
        boolean result = false;

        try   {
            File logFile = new File(appConfig.getProperty("log.path") + "/" + Constants.LOG_FILE);
            result = logFile.renameTo(new File(appConfig.getProperty("log.path") + "/" + DviParameters.getInstance().getLogFileName()));
        }
        catch(Exception e)   {
            logger.error(ILogger.TAG_EXECUTOR, "Error moving file: " + e.getMessage());
        }

        return result;
    }

    /**
     * Set min log level
     *
     * If not configured or configured incorrectly, uses DEBUG
     */
    public void setLogLevel()   {
        String logLevel = appConfig.getProperty("log.level");

        LogEntry.Level enumLevel = null;

        try   {
            enumLevel = LogEntry.Level.valueOf(logLevel.toUpperCase());
        }
        catch(Exception e)   {
            logger.warn("EXEC", "log.level value improperly configured: " + logLevel);
            enumLevel = LogEntry.Level.DEBUG;
        }

        LoggerManager.setMinOutputToWrite(enumLevel);
    }

    /**
     * Stop dvi services and roll the logs.  Additionally, schedule an app stop in two seconds.  This gives the client
     * a response so that they can navigate to a new page via javascript
     *
     * @return boolean
     */
    public boolean autoRollLogs()   {

        stop();
        // let things stop gracefully
        try   {
            Thread.sleep(1000);
        }
        catch(Exception e) {};
        setRecordData(false);

        boolean bResult = moveLogs();

        scheduledExecutor.schedule(new AppStop(), 3, TimeUnit.SECONDS);

        return bResult;
    }

}
