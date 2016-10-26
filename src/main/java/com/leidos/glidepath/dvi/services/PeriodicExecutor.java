package com.leidos.glidepath.dvi.services;


import com.leidos.glidepath.IConsumerTask;
import com.leidos.glidepath.appcommon.*;
import com.leidos.glidepath.appcommon.utils.ConversionUtils;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.asd.spat.SpatMessage;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.dvi.domain.DviParameters;
import com.leidos.glidepath.dvi.domain.GlidepathState;
import com.leidos.glidepath.dvi.domain.GlidepathStateModel;
import com.leidos.glidepath.dvi.domain.MotionStatus;
import com.leidos.glidepath.ead.ITrajectory;
import com.leidos.glidepath.filter.DataFilterFactory;
import com.leidos.glidepath.filter.IDataFilter;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;
import com.leidos.glidepath.ucr.UcrGuiServer;
import com.leidos.glidepath.xgv.XgvConnection;
import com.leidos.glidepath.xgv.XgvConsumer;
import com.leidos.glidepath.xgv.XgvSpeedController;
import com.leidos.glidepath.xgv.XgvStatus;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The PeriodicExecutor
 */
public class PeriodicExecutor implements Callable<DataElementHolder> {

    private static int loop = 0;

    private static ILogger logger = LoggerManager.getLogger(PeriodicExecutor.class);

    private List<IConsumerTask> consumers = new ArrayList<IConsumerTask>();

    private ExecutorService executor = Executors.newFixedThreadPool(5);

    private List<Future<DataElementHolder>> futures = new ArrayList<Future<DataElementHolder>>();

    private ITrajectory eadWrapper;

    private XgvSpeedController xgvSpeedController;

    // if not configured, use 5 cycles
    private int missingDataThreshold = 5;

    // flag used to indicate we have started accelerating after entering ENGAGED state
    //  used to disengage eco drive at stop light
    private boolean bStartedAccel = false;

    // flag used to indicate we have are engaged and have called ead.engaged()
    private boolean bEngaged = false;

    private double lastSpeed = 0.0;

    /**
     * This variable is used to ensure we maintain the information that XgvStatus.manualOverrideEngaged was
     * triggered once we start ecoDrive.
     *
     * As we are not providing every time step info to the UI, we need to know that manualOverrideEngaged was set to
     * true at any point once we are ENGAGED.  This way, the client is assured of getting this information even
     * when it occurs in a time step UIMessage not delivered to the client
     */
    private boolean bOverrideEngaged = false;

    // holds the last X speeds, configurable via exec.speed.queue
    //   all must be zero to stop
    private Queue<Double> speedQueue = new LinkedList<Double>();
    private int speedQueueSize = 5;

    private IDataFilter filter;

    private UcrGuiServer ucrGuiServer;

    /**
     * Maintains a queue of status messages that may result in a warning message to the DVI
     * The statusQueue is the compilation of status messages for the last cycleMax cycles
     * If we reach our threshold of cycleThreshold (3 warnings/etc. within 20 cycles), we will
     * use the previousStatusQueue to display those messages for the next cycleMax cycles
     *
     * If we reach cycleMax without accumulating cycleThreshold statusMessages, we use an empty
     * previousStatusQueue to no longer display status messages on the DVI
     *
     * Any Consumer can provide a status message to the DVI by creating a StringBufferDataElement using
     * DataElementKey.STATUS_MESSAGE
     *
     *  StringBuffer sb = new StringBuffer();
     *  sb.append("There was some error i wanna report to exec/DVI")
     *  holder.put(DataElementKey.STATUS_MESSAGE, new StringBufferDataElement(sb));
     *
     *  Multiple messages can be appended to the element either appending to the StringBuffer prior to creating the
     *  element or using the holder.appendStatusMessage if the element has already been created.
     *
     * Any STATUS_MESSAGE element provided by a Consumer is removed from that consumers holder and appended to the
     * Executors STATUS_MESSAGE element.
     */
    private Queue<String> statusQueue = new LinkedList<String>();
    private Queue<String> previousStatusQueue = new LinkedList<String>();
    private int cycle = 0;

    /**
     * Default values overridden if they appear in the dvi.properties file
     * The default configuration indicates if we have 3 statusMessages within 20 cycles, we will display the
     * messages in the DVI for the next 20 cycles
     */
    private int cycleThreshold = 3;
    private int cycleMax = 20;

    /**
     * Constructor with provided eadWrapper
     *
     * @param eadWrapper
     */
    public PeriodicExecutor(ITrajectory eadWrapper)   {
        this.eadWrapper = eadWrapper;
        String strThreshold = GlidepathApplicationContext.getInstance().getAppConfig().getProperty("missingDataThreshold");
        if (strThreshold != null)    {
            int nThreshold = Integer.parseInt(strThreshold);
            missingDataThreshold = ( nThreshold == 0 ) ? missingDataThreshold : nThreshold;
        }

        AppConfig appConfig = GlidepathApplicationContext.getInstance().getAppConfig();

        this.cycleMax = appConfig.getDefaultIntValue("cycleMax", this.cycleMax);
        this.cycleThreshold = appConfig.getDefaultIntValue("cycleThreshold", this.cycleThreshold);
        this.speedQueueSize = appConfig.getDefaultIntValue("exec.speed.queue", this.speedQueueSize);

        String filterName = appConfig.getProperty("datafilter");
        filter = DataFilterFactory.newInstance(filterName);
        filter.initialize(appConfig.getPeriodicDelay() * Constants.MS_TO_SEC);
        logger.infof(ILogger.TAG_EXECUTOR, "///// Instantiating data filter: %s", filter.getClass().getSimpleName());

        // Initialize UCR GUI server if needed, we'll give it one chance to initialize
        if (appConfig.getUcrEnabled()) {
            logger.info("", "///// UCR HMI OUTPUT ENABLED /////");
            try {
                ucrGuiServer = new UcrGuiServer();
                logger.debug("", "UcrGuiServer=" + ucrGuiServer);
            } catch (IOException e) {
                logger.info("", "Unable to open UcrGuiServer " + e.getMessage());
                ucrGuiServer = null;
            }
        } else {
            logger.info("", "///// UCR HMI OUTPUT DISABLED //////");
        }
    }

    /**
     * Device read cycle - loops through each consumer and submits and then gets future
     *
     * @return  DataElementHolder
     */
    public DataElementHolder call()   {

        DataElementHolder holder = new DataElementHolder();

        // add an empty status message to holder
        holder.put(DataElementKey.STATUS_MESSAGE, new StringBufferDataElement(new StringBuffer()));

        if (xgvSpeedController == null)   {
            xgvSpeedController = instantiateXgvSpeedController();
        }

        // new futures list
        futures.clear();

        // submit tasks
        for (IConsumerTask consumer : consumers)   {
            Future<DataElementHolder> futureHolder = executor.submit(consumer);
            futures.add(futureHolder);
        }

        // collect results
        for (int i=0; i<futures.size(); i++)   {
            Future<DataElementHolder> future = futures.get(i);
            //add all elements from this DataElementHolder
            try   {
                DataElementHolder collectedData = future.get();
                //logger.debug(ILogger.TAG_EXECUTOR, "**** This Consumer got: " + collectedData.toString());

                // any consumer can provide a STATUS_MESSAGE element.  However, we will add any value provided
                //  by the consumer to the primary executor element
                DataElement statusElement = collectedData.remove(DataElementKey.STATUS_MESSAGE);
                if (statusElement != null)   {
                    String tmpStatusMessage = ( (StringBufferDataElement) statusElement).value();
                    if (tmpStatusMessage != null && tmpStatusMessage.length() > 0)   {
                        holder.appendStatusMessage(tmpStatusMessage);
                    }
                }

                holder.putAll(collectedData);
            }
            catch(Exception e)   {
                // log.error
                logger.error(ILogger.TAG_EXECUTOR, "Exception getting future: " + e.getMessage());
            }
        }

        // TODO: asd or ead call to get dtsb and lane id

        // ead also needs operating speed, so load into holder
        double operatingSpeedMph = DviParameters.getInstance().getOperatingSpeed();

        DataElement operatingSpeed = new DoubleDataElement(ConversionUtils.getInstance().mphToMps(operatingSpeedMph));
        holder.put(DataElementKey.OPERATING_SPEED, operatingSpeed);

        double smoothedSpeed = getSmoothedSpeed(holder);

        // calculate motion status and add to holder
        // now acquire last speed as the first element in the speedQueue
        Double javaDouble = speedQueue.peek();
        if (javaDouble == null)   {
            lastSpeed = 0;
        }
        else   {
            lastSpeed = javaDouble.doubleValue();
        }
        DataElementHolder motionStatusHolder = computeMotionStatus(lastSpeed, smoothedSpeed);
        holder.putAll(motionStatusHolder);

        double acceleration = filter.getSmoothedDerivative();
        holder.put(DataElementKey.ACCELERATION, new DoubleDataElement(acceleration) );

        double jerk = filter.getSmoothedSecondDerivative();
        holder.put(DataElementKey.JERK, new DoubleDataElement(jerk));

        // TODO: this may be moved once lane id available outside EAD
        DataElement dataElement = holder.get(DataElementKey.SPAT_MESSAGE);
        if (dataElement != null)   {
            SpatMessageDataElement spatElement = (SpatMessageDataElement) dataElement;
            SpatMessage spat = spatElement.value();
            DataElementHolder spatHolder = getSpatForLane(holder, spat);
            holder.putAll(spatHolder);
        }

        // compute target speed
        DataElementHolder eadHolder = null;
        try   {
            DateTime eadCommandStart = new DateTime();
            eadHolder = eadWrapper.getSpeedCommand(holder);

            Duration duration = new Duration(new DateTime(eadCommandStart), new DateTime());
            holder.put(DataElementKey.CYCLE_EAD, new IntDataElement((int) duration.getMillis()));

            holder.putAll(eadHolder);
        }
        catch(Exception e)   {
            // TODO: SHOULD WE DISENGAGE IF ENGAGED????
            logger.warn(ILogger.TAG_EXECUTOR, "Exception trapped from eadWrapper.getSpeedCommand.");
        }

        boolean isValidated = false;

        // check whether override was EVER triggered once we were ENGAGED
        DataElement xgvStatusElement = holder.get(DataElementKey.XGV_STATUS);
        if (xgvStatusElement != null)   {
            XgvStatus xgvStatus = ((XgvStatusDataElement) xgvStatusElement).value();
            if (bOverrideEngaged)   {
                xgvStatus.setManualOverrideEngaged(true);
                holder.remove(DataElementKey.XGV_STATUS);
                holder.put(DataElementKey.XGV_STATUS, new XgvStatusDataElement(xgvStatus));
            }
        }
        // else, we are not communicating with the xgv, i.e. turned off XGV key
        else   {
            GlidepathStateModel.getInstance().changeState(GlidepathState.FATAL, null);
        }

        // Direct XVG
        GlidepathState state = GlidepathStateModel.getInstance().getState();

        // we only direct if in ENGAGED state
        if ( state.equals(GlidepathState.ENGAGED) )   {
            if (!bEngaged)   {
                bEngaged = true;
                eadWrapper.engage();
            }

            // check to see if we stepped on brake or hit yellow button
            xgvStatusElement = holder.get(DataElementKey.XGV_STATUS);
            if (xgvStatusElement != null)   {
                XgvStatus xgvStatus = ((XgvStatusDataElement) xgvStatusElement).value();

                // this is true when brake or yellow button pressed
                if (xgvStatus.isManualOverrideEngaged())   {
                    // set the flag so we know manual override has been triggered, even if in message not provided to
                    // client
                    bOverrideEngaged = true;

                    // DISENGAGE eco drive
                    GlidepathStateModel.getInstance().changeState(GlidepathState.DISENGAGED, null);
                    bStartedAccel = false;
                    bEngaged = false;
                }
            }

            isValidated = holder.validate();

            //if (xgvSpeedController != null && isValidated)   {
            //if (xgvSpeedController != null)   {
            double currentSpeed = holder.getDoubleElement(DataElementKey.SPEED);

            if (currentSpeed > 0.0)   {
                bStartedAccel = true;
            }

            if (speedQueue.size() >= speedQueueSize)   {
                speedQueue.remove();
            }

            speedQueue.add(new Double(currentSpeed));

            if (!isSpeedZero() )   {
                try   {
                    if (bEngaged)   {
                        double speedCommand = holder.getDoubleElement(DataElementKey.SPEED_COMMAND);
                        DateTime xgvCommandStart = new DateTime();

                        if (xgvSpeedController != null)   {
                            xgvSpeedController.sendMotionProfile(speedCommand);
                        }

                        Duration duration = new Duration(new DateTime(xgvCommandStart), new DateTime());
                        holder.put(DataElementKey.CYCLE_XGV_COMMAND, new IntDataElement((int) duration.getMillis()));
                    }
                }
                catch(Exception e)   {
                    logger.error(ILogger.TAG_EXECUTOR, "Error sendMotionProfile to XGV: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            else   {
                // DISENGAGE eco drive
                GlidepathStateModel.getInstance().changeState(GlidepathState.DISENGAGED, null);
                bStartedAccel = false;
                bEngaged = false;
            }
        }

        // NOTE: This line is used to induce errors to test DVI in simulated mode
        //induceErrors(holder);

        // determine whether we provide status to DVI
        String strStatus = ((StringBufferDataElement) holder.get(DataElementKey.STATUS_MESSAGE)).value();

        // if we have a status message, add it to the queue
        if (strStatus != null && strStatus.length() > 0)   {
            if (statusQueue.size() >= cycleThreshold)   {
                statusQueue.remove();
                statusQueue.add(strStatus);
            }
            else   {
                statusQueue.add(strStatus);
            }
        }

        // condition met to provide status message to DVI
        if (statusQueue.size() >= cycleThreshold && cycle < cycleMax)   {
            logger.warn(ILogger.TAG_EXECUTOR, getMessagesFromQueue(statusQueue).toString());
            resetQueues();
        }
        else if (cycle >= cycleMax)   {
            resetQueues();
        }
        else   {
            cycle += 1;
        }

        // finally, we are always providing the previousStatusQueue to the holder, which should display for cycleMax
        //   so we delete the current element and provide the contents of the previousStatusQueue
        holder.remove(DataElementKey.STATUS_MESSAGE);
        addStatusMessagesToHolder(holder);

        loop += 1;

        // Send data if it is needed and merited
        if (ucrGuiServer != null) {
            logger.debug("", "Sending data to UCR HMI application...");
            
            if (!holder.validate()) {
                logger.debug("", "Data holder incomplete, sending values anyway...");
            }

            try {
                ucrGuiServer.send(holder);
            } catch (IOException e) {
                logger.caughtExcept("", "Unable to send packet to UCR client.", e);
            }
        }

        return holder;
    }

    /**
     * Obtain spat data for lane identified by LANE_ID.  If the value does not exist, we set to lane 12 and log a
     * warning.
     *
     * @param holder
     * @param spat
     * @return DataElementHolder containing the spat info for LANE_ID
     */
    private DataElementHolder getSpatForLane(DataElementHolder holder, SpatMessage spat)   {
        DataElementHolder spatHolder;

        int laneId = holder.getIntElement(DataElementKey.LANE_ID);

        if (laneId != 0)   {
            spatHolder = spat.getSpatForLane(laneId);
        }
        // force to 12
        else   {
            spatHolder = spat.getSpatForLane(15);
            // TODO: Remove hard coding with available outside EAD
            //logger.warn(ILogger.TAG_EXECUTOR, "No LANE_ID provided, setting to lane 12.");
        }

        return spatHolder;
    }

    /**
     * speedQueue MUST be full (speedQueueSize elements) and they must all be zero
     *
     * @return boolean
     */
    private boolean isSpeedZero()   {
        boolean bResult = false;

        // forgot, we need to wait until we have started accelerating
        if (!bStartedAccel)   {
            return bResult;
        }

        if (speedQueue.size() >= speedQueueSize)   {

            for (Double speed : speedQueue)   {
                if (speed != 0)   {
                    return false;
                }
            }

            bResult = true;
        }

        return bResult;
    }


    private void resetQueues()   {
        if (statusQueue.size() < cycleThreshold)   {
            previousStatusQueue.clear();
        }
        else   {
            previousStatusQueue = statusQueue;
        }

        statusQueue = new LinkedList<String>();
        cycle = 0;
    }


    private String addStatusMessagesToHolder(DataElementHolder holder)   {
        StringBuffer sb = getMessagesFromQueue(previousStatusQueue);

        holder.put(DataElementKey.STATUS_MESSAGE, new StringBufferDataElement(sb));

        return sb.toString();
    }


    private StringBuffer getMessagesFromQueue(Queue<String> queue)   {
        StringBuffer sb = new StringBuffer();

        for (String value : queue)   {
            sb.append(value);
        }

        return sb;

    }


    /**
     * Adds current speed to filter and adds a SMOOTHED_SPEED element to the holder
     *
     * @param currentHolder
     * @return  double smoothed speed
     */
    private double getSmoothedSpeed(DataElementHolder currentHolder)   {

        double currentSpeed = currentHolder.getDoubleElement(DataElementKey.SPEED);

        filter.addRawDataPoint(currentSpeed);
        double smoothedSpeed = filter.getSmoothedValue();

        currentHolder.put(DataElementKey.SMOOTHED_SPEED, new DoubleDataElement(smoothedSpeed));

        return smoothedSpeed;
    }


    /**
     * Computes motion status based on lastSpeed and currentSpeed using a configured smoothing factor
     *
     * Last speed is computed as an average of the last X speeds, depending on the size of the configured queue
     * The smoothing factor is specified in meters per second
     *
     * @param lastSpeed double
     * @param currentSpeed double
     * @return
     */
    private DataElementHolder computeMotionStatus(double lastSpeed, double currentSpeed)   {
        MotionStatus motionStatus;

        String strSmoothing = GlidepathApplicationContext.getInstance().getAppConfig().getProperty("motion.status.smoothing");
        double motionStatusSmoothing = Double.parseDouble(strSmoothing);

        logger.debug("ComputeMotionStatus", " #### factor : lastSpeed : currentSpeed    ::: " + strSmoothing + " : " + lastSpeed + " : " + currentSpeed);

        if (lastSpeed == 0.0)   {
            motionStatus = MotionStatus.Stopped;
        }
        else if (currentSpeed > lastSpeed + motionStatusSmoothing)   {
            motionStatus = MotionStatus.Speeding_Up;
        }
        else if (currentSpeed < lastSpeed - motionStatusSmoothing)   {
            motionStatus = MotionStatus.Slowing_Down;
        }
        else   {
            motionStatus = MotionStatus.Coast;
        }

        DataElementHolder holder = new DataElementHolder();
        holder.put(DataElementKey.MOTION_STATUS, new MotionStatusDataElement(motionStatus));

        return holder;
    }


    /**
     * Stop the local executor and release thread resources
     */
    public void stop()   {
        // consumer termination handled above by DviExecutorService

        executor.shutdown();
    }


    /**
     * Set the list of consumers
     *
     * @param consumers
     */
    public void addConsumers(List<IConsumerTask> consumers)   {
        this.consumers = consumers;
    }

    /**
     * clear the consumer list
     */
    public void clearConsumers()   {
        consumers.clear();
    }

    /**
     * Instantiate an XgvSpeedController object
     *
     * @return  xgvSpeedController
     */
    private XgvSpeedController instantiateXgvSpeedController()   {
        XgvSpeedController xgvSpeedController = null;

        try   {
            for (IConsumerTask consumer : consumers)   {
                if (consumer instanceof XgvConsumer)   {
                    XgvConnection connection = ((XgvConsumer) consumer).getConnection();

                    xgvSpeedController = new XgvSpeedController(connection);
                    logger.debug("EXEC", "Instantiated XgvSpeedController.");
                }
            }
        }
        catch(Exception e)   {
            logger.error("EXEC", "Error instantiating an XgvSpeedController:" + e.getMessage());
        }

        return xgvSpeedController;
    }


    /**
     * Used as a mechanism to test error display for simulated DVI
     *
     * @param holder
     */
    private void induceErrors(DataElementHolder holder)   {
        if ( (loop > 100 && loop < 140) || loop > 200 && loop < 240)  {
            if (cycle == 2 || cycle ==4 || cycle == 18)   {
                holder.appendStatusMessage("Some status message for loop starting: " + loop);
            }
        }
    }
}
