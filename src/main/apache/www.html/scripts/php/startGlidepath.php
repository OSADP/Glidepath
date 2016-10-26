<?php
	$cmd = "/opt/glidepath/run-glidepath.sh";
	$outputfile = "/var/www/html/data/logfile.txt";
	$pidfile = "/var/www/html/data/pid.txt";

	shell_exec("rm $outputfile");

	$pid = shell_exec("ps aux | grep java | grep -v grep");

	if ($pid == "") {
		exec(sprintf("%s > %s 2>&1 & echo $! >> %s", $cmd, $outputfile, $pidfile));
		echo "Done starting Glidepath!";
	} else {
		echo "Glidepath is already running!";
	}
?>
