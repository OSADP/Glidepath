/*
 * Web interface for Leidos @ TFHRC Task 4
 * 
 * Retrieves and displays data from the vehicle's internal computer.
 * Designed to be accessed from a mobile device inside the vehicle connected
 * to the vehicle's system WiFi network. Served over Apache 2 webserver.
 *
 * Kyle Rush <kyle.rush@leidos.com>
 */

// Global Constants
var TIMEOUT_MS = 2000;

function refreshData() {
	// Grab the XML data from the server and refresh the page's contents

	// For the moment we assume the browser has AJAX capability (IE7+, etc.)
	// If necessary we can downgrade gracefully later
	var ajaxRequest = new XMLHttpRequest();

	// Set up the callback to be executed when the AJAX call returns
	ajaxRequest.onreadystatechange = function() {

	    var content = document.getElementById("contentDisplay");

	    // Process the result if the AJAX transaction is complete
	    if (ajaxRequest.readyState == 4) {

			if (ajaxRequest.status == 200) {
				// Call was a success, process the XML resoponse and update the data contained in the webpage
				content.innerHTML = parseXMLData(ajaxRequest.responseXML);
			} else {
				// Some error occurred, we'll try again after the timeout
				displayErrorState();
			}
	    }

	}

	// Fire off the asynchronous call
	ajaxRequest.open("GET", "data.xml", true);

	try {
		ajaxRequest.send();
	} catch (e) {
		displayErrorState();
	}

	// Set up the timed interval to call again at
	setTimeout(refreshData, TIMEOUT_MS)
}


function parseXMLData(xmlData) {
	// Parses the data retrieved by an AJAX call and converts it into the HTML 
	// to be displayed. Expects an XML document consisting of tags named <dataEntry>
	// with attributes name, unit, and value as strings. Assigns each dataEntry element
	// to it's own div in the HTML with class "data".

	var generatedHTML = "";

	var entries = xmlData.getElementsByTagName("dataEntry");

	for (i = 0; i < entries.length; i++) {
		// Generate a div for this entry
		var generatedDiv = ""; 

	    // Dynamically tablify the data
	    if (i % 3 === 0) {
		    generatedDiv += "<div class=\"row\">";
		}

	    generatedDiv += "<div class=\"data\">";
		generatedDiv += "<span class=\"tileHeader\">";
		generatedDiv += entries[i].getAttribute("name");
		generatedDiv += "</span>";
		generatedDiv += "<span class=\"tileValue\">";
		generatedDiv += entries[i].getAttribute("data");
		generatedDiv += "</span>";
		generatedDiv += "<span class=\"tileUnit\">";
		generatedDiv += entries[i].getAttribute("unit");
		generatedDiv += "</span>";
		generatedDiv += "</div>";

	    // Close the table row
        if (i % 3 === 2) {
		    generatedDiv += "</div>"
		}

		// Append it to the other divs
		generatedHTML += generatedDiv;
	}

	return generatedHTML;
}

function displayErrorState() {
	// Write an error message into the content div
	document.getElementById("contentDisplay").innerHTML = "<div class=\"error\">ERROR RETRIEVING SERVER DATA. RETRYING...</div>";
}

// Begin refreshing the data once the rest of the HTML is finished loading
window.onload=refreshData();

