function refreshOutput () {
	// Check to see if the user has scrolled up on the page
	var scrolled = false;
	if ($(window).scrollTop() != $(document).height() - $(window).height()) {
		scrolled = true;
	}

	// Grab the log file contents and dump them into #textDisplay
	$("#textDisplay").load("data/logfile.txt");

	// Rescroll down to the bottom
	if (!scrolled) {
		$(window).scrollTop($(document).height() - $(window).height());
	}
}

setInterval(refreshOutput, 500);
