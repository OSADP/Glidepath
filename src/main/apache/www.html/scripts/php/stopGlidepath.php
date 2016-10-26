<?php
	$cmd = "killall java 2>&1";
	$output = exec($cmd);
	if ($output == "") {
		echo "Successfully stopped Glidepath.";
	} else {
		echo $output;
	}
?>