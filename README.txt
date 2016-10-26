Open Source Overview
============================
GlidePath Eco-Approach and Departure
Version 1.6.5

Description:
The GlidePath software is written in Java and is intended to run on an Ubuntu 14.04 system onboard the test vehicle.  It is based on the Spring framework, using Spring Boot to run all operations as a single web server, with the service bus, web pages and interface code all bundled into a single executable jar file.


Installation and removal instructions
-------------------------------------
The software builds into a single jar file, which should be installed on the vehicle onboard computer under a directory named /opt/glidepath.  There also needs to be a directory named /opt/glidepath/logs for it to write log files into.  Finally, in the /opt/glidepath directory there can optionally be a configuration file, named dvi.properties, that allow an operator to customize a variety of configuration parameters.  A default version of this file is embedded in the jar file, which the application will use unless it finds this file in the /opt/glidepath directory.  To uninstall the software, simply delete this directory and its contents.


License information
-------------------
See the accompanying LICENSE file.


System Requirements
-------------------------
Minimum memory:  2 GB
Processing power:  Intel Core I3 @ 1.6 GHz or equivalent
Connectivity:  ethernet
Operating systems supported:  Ubuntu 14.04


Documentation
-------------
The following documentation is available upon request from FHWA:
Requirements Specification
System Design Document
Test Plan
Test Report
Project Final Report


Web sites
---------
The software is distributed through the USDOT's JPO Open Source Application Development Portal (OSADP)
http://itsforge.net/ 
