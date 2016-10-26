Apache Web Start readme

Place the files, in directory hierarchy, in the /var/www/html apache directory (default html dir).

All files should be dos2unix'd once placed.

Currently, the startGlidepath php script executes the run-glidepath.sh script, looking for it in the /opt/glidepath
directory.  It should also be dos2unix'd.

The reason for the script, is so the the speedcontrol jar is started from the /opt/glidepath directory so that
it can use the file system dvi.properties.

The Glidepath Control Panel is located at http://192.168.0.4
