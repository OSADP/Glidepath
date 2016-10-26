/*global displayDynamicTable,jsonQueryFieldDefs*/


function _DviAjax() {}

var DviAjax = new _DviAjax();

_DviAjax.prototype.speeds = [];
_DviAjax.prototype.targetSpeeds = [];

_DviAjax.prototype.navigationPage = "http://192.168.0.4";
//_DviAjax.prototype.navigationPage = "http://localhost";
_DviAjax.prototype.endExperimentPage = "endExperiment.html";
_DviAjax.prototype.manualOverridePage = "manualOverride.html";

/**
 * REST endpoints to start or stop Consumers
 *
 * @param restEndpoint
 * @param id
 */
_DviAjax.prototype.ajaxStartStop = function (restEndpoint, id) {
    "use strict";

    var request = $.ajax({
        url : restEndpoint,
        dataType : "json",
        type : "get",
        data : {
            device : "all"
        }
    });


    request.done(function(response, textStatus, jqXHR) {
        var id = document.getElementById(DviConstants.ID_SERVER_MESSAGE);
        id.innerHTML = response.serverMessage;
    });

    request.fail(function(jqXHR, textStatus, errorThrown) {
        alert("An unknown error occurred for Endpoint:" + restEndpoint);
    });

}


/**
 * Start eco drive from status page
 */
_DviAjax.prototype.ajaxStartEcoDrive = function () {
    "use strict";

    var operatingSpeed = $( "#operatingSpeedId" ).val();
    var logFilePrefix = $( "#logFilePrefixId" ).html();


    var request = $.ajax({
        url : "setDviParameters",
        dataType : "json",
        type : "post",
        data : {
            operatingSpeed : operatingSpeed,
            logFilePrefix : logFilePrefix
        }
    });


    request.done(function(response, textStatus, jqXHR) {
        if ( response.result )   {
            window.location = "/driverView"
        }

    });

    request.fail(function(jqXHR, textStatus, errorThrown) {
        alert("An unknown error occurred setting operating speed.");
    });


}


/**
 * REST endpoint for entering or exiting ecoDrive
 *
 * @param state "ENGAGED" or "DISENGAGED"
 */
_DviAjax.prototype.ajaxEcoDrive = function (state) {
    "use strict";

    var on = true;
    if (state === "ENGAGED")   {
        on = false;
    }

    var request = $.ajax({
        url : "setEcoDrive",
        dataType : "json",
        type : "post",
        data : {
            ecoDrive : on
        }
    });


    request.done(function(response, textStatus, jqXHR) {
        if ( response.result )   {

        }

    });

    request.fail(function(jqXHR, textStatus, errorThrown) {
        alert("An unknown error occurred updating EcoDrive.");
    });

}

_DviAjax.prototype.ajaxStringList = function (action) {
    "use strict";

    var request = $.ajax({
        url : "ajaxStringList",
        dataType : "json",
        type : "post",
        data : {
            action : action
        }
    });


    request.done(function(response, textStatus, jqXHR) {
        if ( response.result )   {
            var consumers = response.consumers
            addConsumersToList(consumers);
        }

    });

    request.fail(function(jqXHR, textStatus, errorThrown) {
        alert("An unknown error occurred setting operating speed.");
    });

}

_DviAjax.prototype.ajaxAddConsumer = function () {
    "use strict";

    var select = document.getElementById("consumerListId");

    var consumer = select.options[select.selectedIndex].value;

    var request = $.ajax({
        url : "ajaxAddConsumer",
        dataType : "json",
        type : "post",
        data : {
            consumer: consumer
        }
    });


    request.done(function(response, textStatus, jqXHR) {
        if ( response.result )   {
            var id = document.getElementById(DviConstants.ID_SERVER_MESSAGE);
            id.innerHTML = response.serverMessage;
        }

    });

    request.fail(function(jqXHR, textStatus, errorThrown) {
        alert("An unknown error occurred adding Consumer.");
    });

}

/**
 * REST endpoint for adding Consumers to a list to provide the capability to test individual consumers
 */
_DviAjax.prototype.ajaxAddConsumers = function () {
    "use strict";

    var select =$("#consumerListId");

    var consumers = select.val();

    //var consumer = select.options[select.selectedIndex].value;

    var request = $.ajax({
        url : "ajaxAddConsumers",
        dataType : "json",
        type : "post",
        data : {
            consumers: consumers
        }
    });


    request.done(function(response, textStatus, jqXHR) {
        if ( response.result )   {
            var id = document.getElementById(DviConstants.ID_SERVER_MESSAGE);
            id.innerHTML = response.serverMessage;
        }

    });

    request.fail(function(jqXHR, textStatus, errorThrown) {
        alert("An unknown error occurred adding Consumer.");
    });

}

/**
 * REST endpoint to acquire either speed or target speed from server log file speeds.csv
 *
 * @param speedType
 */
_DviAjax.prototype.ajaxGetSpeeds = function (speedType) {
    "use strict";

    var request = $.ajax({
        url : "speeds",
        dataType : "json",
        type : "post",
        data : {
            speedType: speedType
        }
    });


    request.done(function(response, textStatus, jqXHR) {
        if ( response.result )   {

            var speeds = response.speeds;

            var points = [];

            var i = 0;
            for (i = 0; i < speeds.length; i++)   {
                var x = speeds[i].x;
                var y = speeds[i].y;
                //var point = [ x, parseFloat(y.toPrecision(3))];
                var point = [ x, y ];
                points.push(point);
            }

            if (speedType == "targetSpeeds")   {
                _DviAjax.prototype.targetSpeeds = points;
            }
            else   {
                _DviAjax.prototype.speeds = points;
            }

            // plot when we have both speeds
            if (_DviAjax.prototype.targetSpeeds.length > 0 &&
                _DviAjax.prototype.speeds.length > 0)   {

                var dataset = [
                    { label: "Speed", data: DviAjax.speeds },
                    { label: "Target Speed", data: DviAjax.targetSpeeds }

                ];

                $.plot($("#chartid"), dataset);

            }

        }

    });

    request.fail(function(jqXHR, textStatus, errorThrown) {
        alert("An unknown error occurred retrieving speeds for graphing.");
    });

}

/**
 * Turn executor data logging on or off
 *
 * @param flag
 */
_DviAjax.prototype.ajaxSetLogging = function (flag, bEndExperiment) {
    "use strict";

    var request = $.ajax({
        url : "setLogging",
        dataType : "json",
        type : "post",
        data : {
            logging : flag
        }
    });


    request.done(function(response, textStatus, jqXHR) {
        if ( response.result )   {
            // if turning on logging, either remove button or change to stop
            if (flag)   {
                var img = document.getElementById("recordDataId");
                img.src = "";
            }
            // if turning off logging, end experiment and navigate to configured page
            else   {
                if (bEndExperiment)   {
                    window.location = _DviAjax.prototype.endExperimentPage;
                }
                else   {
                    window.location = _DviAjax.prototype.manualOverridePage;
                }
            }

        }
    });

    request.fail(function(jqXHR, textStatus, errorThrown) {
        alert("An unknown error occurred for setLogging.");
    });

}

