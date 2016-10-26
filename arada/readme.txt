The following two scripts should be deployed on the Arada box (192.168.0.40)

To copy the files to the Arada:
scp arada-* root@192.168.0.40:/tmp

They are currently deployed to the /etc/glidepath directory on the Arada
and must be manually started.

Once started, you can exit the box.

Syntax:
   ssh root@192.168.0.40
   cd /tmp
   ./arada-map<sys> start     (use stop or restart, too)
   ./arada-spat<sys> start
   exit
where <sys> is either -dev or -live depending on which vehicle PC the data is to be routed to.


Troubleshooting

	Verify ip6 addresses (primarily the dev box)

   Arada	2001:470:1234:1111::1/64
   DEV		2001:470.1234.1111::4/64

Consult AradaNotes.txt for additional details.

==============================================

For the two C programs in this directory:
wsmpforwardserver needs to be running on the Arada OBU to forward the messages to the
Glidepath computer.

The WsmpDump program is optional, and is to be run on the Glidepath computer as a simple
way to verify that this computer is receiving OBU messages as expected.
