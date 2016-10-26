var strEcoDriveState = "";

function setImageVisible(id, visible) {
    var img = document.getElementById(id);
    img.style.visibility = (visible ? 'visible' : 'hidden');
}

function setTrafficSignal(uiMessage) {
    "use strict"

    if (uiMessage.signalPhase != "NONE")    {
        var color = uiMessage.signalPhase;

        // table.backround vice img.src
        var table = document.getElementById(DviConstants.ID_TRAFFIC_SIGNAL);
        var timeNextPhaseElement = document.getElementById("timeNextPhaseId");

        if (color === "RED") {
            // set style, if data < 10, we need to pad a bit more
            if (Math.round(uiMessage.timeNextPhase) < 10)   {
                timeNextPhaseElement.className = "timeNextPhaseRedSingle";
            }
            else {
                timeNextPhaseElement.className = "timeNextPhaseRed";
            }
        }
        else if (color === "GREEN") {
            if (Math.round(uiMessage.timeNextPhase) < 10)   {
                timeNextPhaseElement.className = "timeNextPhaseGreenSingle";
            }
            else {
                timeNextPhaseElement.className = "timeNextPhaseGreen";
            }
        }
        else  {
            // yellow always less than 10, so don't need different styles
            timeNextPhaseElement.className = "timeNextPhaseYellow";
        }

        setDviData("timeNextPhaseId", Math.round(uiMessage.timeNextPhase));
    }

}


function setDistanceToStopBar(uiMessage)   {
    "use strict"

    if (uiMessage.distanceToStopBar)   {
        var distance = uiMessage.distanceToStopBar * -1;

        $( "#distanceToStopBarSlider" ).slider( "value", distance );
    }

}


function setDviStatusMessage(uiMessage)   {
    "use strict"

    var statusMessage = "";

    // change ANY message filled by Exec or Consumers to the fatal glidepath warning
    if (uiMessage.statusMessage.length > 0)   {
        statusMessage = "Glidepath system unavailable; resume manual control.";
    }

    setDviData("statusMessageId", statusMessage);

}


function setDviData(theId, theData)   {
    var id = document.getElementById(theId);
    id.innerHTML = theData;

}

/**
 * Set gear shift image based on state
 *
 * @param uiMessage
 */
function setGearStateImage(uiMessage)    {
    "use strict"

    var img = document.getElementById("gearShiftImageId");

    if (uiMessage.gearState)   {
        var gear = uiMessage.gearState;

        if (gear === "NEUTRAL") {
            img.src = "images/greenCircle.png";
        }
        else {
            img.src = "images/redCircle.png";
        }
    }
    else   {
        img.src = "images/redCircle.png";
    }
}

/**
 * When we have reach a certain distance past the stop bar, auto roll logs
 *
 * @param uiMessage
 */
function checkAutoStop(uiMessage)    {
    "use strict"

    if (uiMessage.distanceToStopBar && uiMessage.dtsbAutoStop)   {
        var dtsb = parseFloat(uiMessage.distanceToStopBar);
        if (dtsb <= uiMessage.dtsbAutoStop)    {
            // second arg indicates navigate to endExperimentPage
            DviAjax.ajaxSetLogging(false, true);
        }
    }
}

function setEcoDriveState(uiMessage)    {
    "use strict"

    var theButton = document.getElementById("ecoDriveId");

    var FLASH_IMAGE = "images/greenGoFlash.png";
    var ON_IMAGE = "images/greenGo.png";

    var state = uiMessage.glidepathState;
    strEcoDriveState = state;

    // this is now used just for GO button we no longer display a button to DISENGAGE
    if (state === "DISENGAGED") {
        if (uiMessage.signalPhase === "RED" && uiMessage.timeNextPhase <= 5)   {
            playSound("audioId");
            if (theButton.src.indexOf(FLASH_IMAGE) === -1)   {
                theButton.src = FLASH_IMAGE;
            }
            else   {
                theButton.src = ON_IMAGE;
            }
        }
        else   {
            theButton.src = ON_IMAGE;
        }

    }
}


/**
 * Set activation key image
 *
 * @param uiMessage
 */
function setActivationKeyImage(uiMessage)    {
    "use strict"

    var img = document.getElementById("activationImageId");

    if (uiMessage.activationKey)   {
        var activationKey = uiMessage.activationKey;

        if (activationKey) {
            img.src = "images/greenCircle.png";
        }
        else {
            img.src = "images/redCircle.png";
        }
    }
    else   {
        img.src = "images/redCircle.png";
    }
}

/**
 * Set Emergency Override/Yellow light key
 *
 * @param uiMessage
 */
function setYellowButtonImage(uiMessage)    {
    "use strict"

    var img = document.getElementById("yellowButtonImageId");

    if (uiMessage.manualOverrideEngaged)   {
        img.src = "images/redCircle.png";
    }
    else {
        img.src = "images/greenCircle.png";
    }
}

/**
 * Set GPS Verification
 *
 * @param uiMessage
 */
function setGpsImage(uiMessage)    {
    "use strict"

    var img = document.getElementById("gpsImageId");

    if (uiMessage.distanceToStopBar)   {
        img.src = "images/greenCircle.png";
    }
    else {
        img.src = "images/redCircle.png";
    }
}

/**
 * Set the conditions required to enable the GO button
 *
 * @param uiMessage
 */
function setGoButtonImage(uiMessage)   {
    "use strict"

    var img = document.getElementById("goButtonId");

    if (uiMessage.activationKey && uiMessage.gearState == "NEUTRAL" &&
        uiMessage.distanceToStopBar && !uiMessage.manualOverrideEngaged)   {
        img.src = "images/greenGo.png"
    }
    else  {
        img.src = "";
    }
}


function setMotionStatusImage(uiMessage)    {
    "use strict"

    var img = document.getElementById("motionStatusImageId");

    var motionStatus = uiMessage.motionStatus;

    if (motionStatus === "Speeding_Up") {
        img.src = "images/greenAcceleration.jpg";
    }
    else if (motionStatus === "Slowing_Down") {
        img.src = "images/yellowSlowingDown.jpg";
    }
    else if (motionStatus === "Stopped") {
        img.src = "images/redStopped.jpg";
    }
    else if (motionStatus === "Coast") {
        img.src = "images/blueCoast.jpg";
    }
}

/**
 * Navigate to the web start/stop apache page
 */
function navigateHome()   {
    window.location = DviAjax.navigationPage;
}


function operatingSpeedUp()   {
    changeOperatingSpeed(true);
}

function operatingSpeedDown()   {
    changeOperatingSpeed(false);
}

function changeOperatingSpeed(up)   {
    var min = 10;
    var max = 30;
    var delta = 5;

    var osElement = document.getElementById("operatingSpeedId");

    var speed = parseInt(osElement.value, 10);

    if (up)   {
        speed = speed + delta;
    }
    else   {
        speed = speed - delta;
    }

    if (up)   {
        if (speed > max) {
            speed = max;
        }
    }
    else   {
        if (speed < min)   {
            speed = min;
        }
    }

    osElement.value  = speed;
}

$(window).load(function() {
    connect();
});


$(window).unload(function() {
    disconnect();
});
