<?php
	$pid = shell_exec("ps aux | grep 'java -jar' | grep -v grep");

	if ($pid == "")   {
	    echo "NOT Running";
	}
	else   {
        $state = shell_exec("cat /var/www/html/data/state.dat");
	    echo $state;
	}
?>
