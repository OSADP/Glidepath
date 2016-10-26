// get speed and target speed lists from server

$(document).ready(function() {
    "use strict"

    DviAjax.ajaxGetSpeeds("speeds");
    DviAjax.ajaxGetSpeeds("targetSpeeds");

});
