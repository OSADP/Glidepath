
/**
 * add a customized vehicle marker at the provided lat long, removing previous marker first
 * @param lat
 * @param long
 */
function addMarker(lat, long)   {
    if (marker)  {
        marker.setMap(null);
    }

    //Ford Escape marker
	var carImage = {
        url: '\images/FordEscape.png',
        // Scale the marker to 50 pixels wide by 25 pixels high.
        scaledSize: new google.maps.Size(120, 60),
        // The origin for this image is (0, 0).
        origin: new google.maps.Point(0, 0),
        // The anchor for this image is the base of the flagpole at (0, 10).
        anchor: new google.maps.Point(10, 40)
    };
    marker = new google.maps.Marker( {
        position: new google.maps.LatLng(lat, long),
        icon: carImage,
        draggable: true,
        map: map
    } );

}


$(document).ready(function() {
    // map of saxton lab and experiment road
    map = new google.maps.Map(document.getElementById('map'), {
        zoom: 10,
        center: new google.maps.LatLng(38.955289, -77.148694),
        mapTypeId: google.maps.MapTypeId.HYBRID
    });

    // location of the stop bar
	var trafficLight = {
        url: '\images/trafficLight2.png',
        // Scale the marker
        scaledSize: new google.maps.Size(38, 102),
        // The origin for this image is (0, 0).
        origin: new google.maps.Point(0, 0),
        // Set the anchor for this image
        anchor: new google.maps.Point(0, 110)
    };
    var stopBarMarker = new google.maps.Marker({
        position: new google.maps.LatLng(38.954930, -77.149170),
		icon: trafficLight,
        map: map
    });
	
	// location of the stop bar text
	var stopbarTxt = {
        url: '\images/stopbarText.png',
        // Scale the marker
        scaledSize: new google.maps.Size(240, 120),
        // The origin for this image is (0, 0).
        origin: new google.maps.Point(0, 0),
        // Set the anchor for this image
        anchor: new google.maps.Point(90, 220)
    };
    var stopBarTxtMarker = new google.maps.Marker({
        position: new google.maps.LatLng(38.954930, -77.149170),
		icon: stopbarTxt,
        map: map
    });
	
    map.setZoom(zoomLevel);

});
