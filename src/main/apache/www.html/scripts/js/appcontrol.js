/*
 * Web interface for Leidos @ TFHRC Task 4
 * 
 * Communicates with a PHP script running on the vehicle to start and stop
 * long running processes such as speed harmonization or cooperative
 * adaptive cruise control.
 *
 * Kyle Rush <kyle.rush@leidos.com>
 */
var URL_DVI = "http://192.168.0.4:8080";     // production
//var URL_DVI = "http://192.168.56.103:8080";    // dev VM

var POLLING_SLEEP = 1500;

function startGlidepath () {
    $.ajax({
        type: 'POST',
        url: "scripts/php/startGlidepath.php"
    }).done(function (data) {
            alert(data);
        });
}

function stopGlidepath () {
    $.ajax({
        type: 'POST',
        url: "scripts/php/stopGlidepath.php"
    }).done(function (data) {
            alert(data);
        });
}

function navigateToDvi () {
    window.location.href = URL_DVI;
}

function pollGlidepath() {
    setTimeout(function()  {
        var control = document.getElementById("dviStatusId");

        $.ajax({
            type: 'POST',
            url: "scripts/php/statusGlidepath.php"
        }).done(function (data) {
                var control = document.getElementById("dviStatusId");
		
		if (data === "STANDBY")   {
			navigateToDvi();
		}

                if (data.indexOf("NOT") == 0)   {
                    control.className = "centeredText redText";
                }  else  {
                    control.className = "centeredText greenText";
                }

                control.innerHTML = data;

                pollGlidepath();
            });
    }, POLLING_SLEEP);
}

$(window).load(function() {
    pollGlidepath();
});
