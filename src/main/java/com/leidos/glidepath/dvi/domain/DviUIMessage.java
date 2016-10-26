package com.leidos.glidepath.dvi.domain;

import com.leidos.glidepath.appcommon.*;
import com.leidos.glidepath.appcommon.utils.ConversionUtils;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;
import com.leidos.glidepath.xgv.messages.ReportDiscreteDevicesMessage;
import com.leidos.glidepath.xgv.XgvStatus;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Date;

/**
 * Models the information needed by the DVI web client
 */
public class DviUIMessage {

    private SimpMessagingTemplate template;
    private static ILogger logger = LoggerManager.getLogger(DviUIMessage.class);

    private GlidepathState glidepathState;
    private SignalPhase signalPhase;
    private double timeNextPhase;
    private double timeThirdPhase;
    private MotionStatus motionStatus;
    private double speed;                   // mph
    private double targetSpeed;             // mph
    private double latitude;
    private double longitude;
    private double distanceToStopBar;       // feet
    private ReportDiscreteDevicesMessage.XgvGearState gearState;
    private boolean activationKey;
    private boolean parkingBrakeSet;
    private boolean safeStopStopEngaged;
    private boolean manualOverrideEngaged;
    private double operatingSpeed;           // mph
    private double dtsbAutoStop;             // distance from stop bar in meters to initiate auto stop
    private String statusMessage;
    private long timeStamp;

    public DviUIMessage()   {
        this.glidepathState = GlidepathState.STARTUP;
        this.signalPhase = SignalPhase.GREEN;
        this.timeNextPhase = 0;
        this.timeThirdPhase = 0;
        this.motionStatus = MotionStatus.Stopped;
        this.speed = 0;
        this.targetSpeed = 0;
        this.latitude = 0;
        this.longitude = 0;
        this.distanceToStopBar = 0;
        this.statusMessage = "";
        this.dtsbAutoStop = -150.0;
        this.timeStamp = new Date().getTime();
        this.manualOverrideEngaged = true;
    }

    public DviUIMessage(GlidepathState glidepathState, SignalPhase signalPhase, double timeNextPhase, double timeThirdPhase,
                        MotionStatus motionStatus, double speed, double targetSpeed, double latitude, double longitude,
                        double distanceToStopBar, String statusMessage, long timeStamp, boolean manualOverrideEngaged)   {
        this.glidepathState = glidepathState;
        this.signalPhase = signalPhase;
        this.timeNextPhase = timeNextPhase;
        this.timeThirdPhase = timeThirdPhase;
        this.motionStatus = motionStatus;
        this.speed = speed;
        this.targetSpeed = targetSpeed;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceToStopBar = distanceToStopBar;
        this.statusMessage = statusMessage;
        this.dtsbAutoStop = -150.0;
        this.timeStamp = timeStamp;
        this.manualOverrideEngaged = manualOverrideEngaged;
    }

    public DviUIMessage(DataElementHolder holder) {
        super();

        try   {
            DataElementKey[] keys = DataElementKey.values();

            String strDtsbAutoStop = GlidepathApplicationContext.getInstance().getAppConfig().getProperty("dtsb.auto.stop");

            try   {
                double tmpDtsbAutoStop = Double.parseDouble(strDtsbAutoStop);
                setDtsbAutoStop(tmpDtsbAutoStop);
            }
            catch(Exception e)   {
                // use default contructor value of -50.0 if value is not interpretatble or is null
            }

            for (DataElementKey key : keys)   {

                switch (key)    {

                    case SPEED:
                        double speed = holder.getDoubleElement(key);
                        setSpeed(ConversionUtils.getInstance().mpsToMph(speed));
                        break;

                    case SPEED_COMMAND:
                        double speedCommand = holder.getDoubleElement(key);
                        setTargetSpeed(ConversionUtils.getInstance().mpsToMph(speedCommand));
                        break;

                    case OPERATING_SPEED:
                        double operatingSpeed = holder.getDoubleElement(key);
                        setOperatingSpeed(ConversionUtils.getInstance().mpsToMph(operatingSpeed));
                        break;

                    case LATITUDE:
                        double latitude = holder.getDoubleElement(key);
                        setLatitude(latitude);
                        break;

                    case LONGITUDE:
                        double longitude = holder.getDoubleElement(key);
                        setLongitude(longitude);
                        break;

                    case DIST_TO_STOP_BAR:
                        double distance = holder.getDoubleElement(key);
                        setDistanceToStopBar(ConversionUtils.getInstance().metersToFeet(distance));
                        break;

                    case SIGNAL_PHASE:
                        DataElement signalPhaseElement = holder.get(key);
                        if (signalPhaseElement != null)   {
                            SignalPhase signalPhase = ((PhaseDataElement) signalPhaseElement).value();
                            setSignalPhase(signalPhase);
                        }
                        else   {
                            setSignalPhase(SignalPhase.NONE);
                        }
                        break;

                    case SIGNAL_TIME_TO_NEXT_PHASE:
                        double nextPhase = holder.getDoubleElement(key);
                        setTimeNextPhase(nextPhase);
                        break;

                    case SIGNAL_TIME_TO_THIRD_PHASE:
                        double thirdPhase = holder.getDoubleElement(key);
                        setTimeThirdPhase(thirdPhase);
                        break;

                    case XGV_STATUS:
                        DataElement xgvStatusElement = holder.get(key);
                        if (xgvStatusElement != null)   {
                            XgvStatus xgvStatus = ((XgvStatusDataElement) xgvStatusElement).value();
                            setActivationKey(xgvStatus.isOn());
                            setGearState(xgvStatus.getGear());
                            setParkingBreakSet(xgvStatus.isParkingBrakeSet());
                            setSafeStopStopEngaged(xgvStatus.isSafeStopStopEngaged());
                            setManualOverrideEngaged(xgvStatus.isManualOverrideEngaged());
                        }
                        break;

                    case MOTION_STATUS:
                        DataElement motionStatusElement = holder.get(key);
                        if (motionStatusElement != null)   {
                            MotionStatus motionStatus = ((MotionStatusDataElement) motionStatusElement).value();
                            setMotionStatus(motionStatus);
                        }
                        break;

                    case STATUS_MESSAGE:
                        DataElement statusMessageElement = holder.get(key);
                        String statusMessage = ((StringBufferDataElement) statusMessageElement).value();
                        setStatusMessage(statusMessage);
                        break;

                }

            }

        }
        catch(Exception e)   {
            logger.warn("DVI", "DVI message conversion issue: " + e.getMessage());
        }

        setGlidepathState(GlidepathStateModel.getInstance().getState());
    }


    /**
     * Setters and Getters
     *
     */

    public void setGlidepathState(GlidepathState glidepathState)  {
        this.glidepathState = glidepathState;
    }

    public void setSignalPhase(SignalPhase signalPhase)   {
        this.signalPhase = signalPhase;
    }

    public void setTimeNextPhase(double seconds)   {
        this.timeNextPhase = seconds;
    }

    public void setTimeThirdPhase(double seconds)   {
        this.timeThirdPhase = seconds;
    }

    public void setMotionStatus(MotionStatus motionStatus)   {
        this.motionStatus = motionStatus;
    }

    public void setSpeed(double speed)   {
        this.speed = speed;
    }

    public void setTargetSpeed(double targetSpeed)   {
        this.targetSpeed = targetSpeed;
    }

    public void setLatitude(double latitude)   {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude)   {
        this.longitude = longitude;
    }

    public void setDistanceToStopBar(double distanceToStopBar)   {
        this.distanceToStopBar = distanceToStopBar;
    }

    public void setGearState(ReportDiscreteDevicesMessage.XgvGearState gearState)   {
        this.gearState = gearState;
    }

    public void setActivationKey(boolean activationKey)   {
        this.activationKey = activationKey;
    }

    public void setParkingBreakSet(boolean parkingBrakeSet)   {
        this.parkingBrakeSet = parkingBrakeSet;
    }

    public void setSafeStopStopEngaged(boolean safeStopStopEngaged)   {
        this.safeStopStopEngaged = safeStopStopEngaged;
    }

    public void setManualOverrideEngaged(boolean manualOverrideEngaged)   {
        this.manualOverrideEngaged = manualOverrideEngaged;
    }

    public void setOperatingSpeed(double operatingSpeed)   {
        this.operatingSpeed = operatingSpeed;
    }

    public void setDtsbAutoStop(double dtsbAutoStop)   {
        this.dtsbAutoStop = dtsbAutoStop;
    }

    public void setStatusMessage(String statusMessage)   {
        this.statusMessage = statusMessage;
    }

    public void setTimeStamp(long timeStamp)   {
        this.timeStamp = timeStamp;
    }

    public GlidepathState getGlidepathState()   {
        return glidepathState;
    }

    public SignalPhase getSignalPhase()   {
        return signalPhase;
    }

    public double getTimeNextPhase()   {
        return timeNextPhase;
    }

    public double getTimeThirdPhase()   {
        return timeThirdPhase;
    }

    public MotionStatus getMotionStatus()   {
        return motionStatus;
    }

    public double getSpeed()   {
        return speed;
    }

    public double getTargetSpeed()   {
        return targetSpeed;
    }

    public double getLatitude()   {
        return latitude;
    }

    public double getLongitude()   {
        return longitude;
    }

    public double getDistanceToStopBar()   {
        return distanceToStopBar;
    }

    public ReportDiscreteDevicesMessage.XgvGearState getGearState()   {
        return gearState;
    }

    public boolean isActivationKey()   {
        return activationKey;
    }

    public boolean isParkingBreakSet()   {
        return parkingBrakeSet;
    }

    public boolean isSafeStopStopEngaged()   {
        return safeStopStopEngaged;
    }

    public boolean isManualOverrideEngaged()   {
        return manualOverrideEngaged;
    }

    public double getOperatingSpeed()   {
        return operatingSpeed;
    }

    public double getDtsbAutoStop()   {
        return dtsbAutoStop;
    }

    public String getStatusMessage()   {
        return statusMessage;
    }

    public long getTimeStamp()   {
        return timeStamp;
    }

    @Override
    public String toString()   {
        return "DviUIMessage [ glidepathState=" + getGlidepathState() +
                ", signalPhase=" + getSignalPhase() +
                ", timeNextPhase=" + getTimeNextPhase() +
                ", timeThirdPhase=" + getTimeThirdPhase() +
                ", motionStatus=" + getMotionStatus() +
                ", speed=" + getSpeed() +
                ", targetSpeed=" + getTargetSpeed() +
                ", latitude=" + getLatitude() +
                ", longitude=" + getLongitude() +
                ", distanceToStopBar=" + getDistanceToStopBar() +
                ", timeStamp=" + getTimeStamp() +
                " ]";
    }

}
