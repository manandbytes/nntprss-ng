nntp//rss - version 0.4
-----------------------

February 8, 2003

Bridging the worlds of NNTP clients and RSS feeds, nntp//rss is an 
application that will enable you to use your existing NNTP 
newsreader to read your favourite RSS syndicated information channels.

Project Home Page: http://www.methodize.org/nntprss
Source Forge Project Page: http://www.sourceforge.net/projects/nntprss

Email: nntprss@methodize.org

Requirements
============

- Java Runtime Environment 
  (nntp//rss has been tested against JDK 1.3.1 and JDK 1.4.1 on
  Win32, and JDK 1.4.1 on Solaris, Red Hat Linux 7.2 and Mac OS X)
  Sun's Java Runtime Environment may be downloaded from:
  http://java.sun.com/j2se/downloads.html
  
- NNTP-based news reader
  nntp//rss has been tested with the following newsreaders:
    - Outlook Express 6
    - Netscape v4.7
    - Mozilla v1.2.1
    - Free Agent v1.92
    - MT-NewsWatcher v3.1 (Mac OS 9), v3.2 (Max OS X)
    - Tin 1.5.8 (RedHat 7.2) (ensure content type is set
        to text/plain in nntp//rss's System Configuration)
    - MacSoup 2.4.6 (Mac OS 9) (ensure content type is set
        to text/plain in nntp//rss's System Configuration)
    - Entourage (Office X - Mac OS X)
    - MicroPlanet Gravity v2.50


Upgrading from version 0.3
==========================

nntp//rss v0.4 uses a new database persistence engine.  This 
provides greater performance, and reduces memory usage.  Follow the
steps below if you wish to migrate your existing articles from 
nntp//rss v0.3.  Note that if you have a large database of articles,
this process may take a some time.

If you do not want to migrate your existing articles, just follow the
steps to export your channel configuration.  You can import this
configuration directly into nntp//rss v0.4.

1. Backup your channel configuration.  Go into nntp//rss's web
   administration interface, select 'System Configuration', then
   click on 'Export Channel List'.  Save the nntprss-channels.xml
   file to a safe location.

2. Stop nntp//rss v0.3

3. Backup your current nntp//rss directory.

4. As v0.4 contains a number of additional libraries and files,
   the easiest way to upgrade is to unpack nntp//rss 0.4 to a 
   separate directory, then copy over your nntp//rss 0.3 database
   files.  These files contain configuration and channel data.

   i. Unpack v0.4 to a new directory
   ii. Copy the following files from the root (nntprss) directory of 
   your nntp//rss v0.3 installation, to the root (nntprss) directory
   of your v0.4 installation:
   
   nntprssdb.properties
   nntprssdb.data
   nntprssdb.script
   nntprssdb.backup

5. If you had changed the NNTP or administrative web interface
   listener ports, you should copy over the nntprss-config.xml
   from your previous version.
   
   Also, if you had configured a users.properties file, for
   web interface security, you should copy this over from your
   previous installation.

6. Start nntp//rss v0.3
   (See below for startup information)

Your existing configuration will be kept.


Installation
============

To install nntp//rss, simply unpack the distribution into a directory
of your choice.


Configuration
=============

Configuration is only necessary at this stage if you want to
change the port on which the NNTP server listens (default 119),
or the port for the web admin interface (default 7810).

Both parameters are stored in the nntprss-config.xml file, found
in the directory where you unpacked nntp//rss.

Simply change the 'port' attribute of either the nntp or admin
elements to your desired port(s).  If you change either of the 
ports after nntp//rss has been started, you will need to stop 
and restart nntp//rss.


Running nntp//rss
=================

In the directory where you unpacked the nntp//rss distribution, execute
the following command:

Windows:

javaw -jar nntprss-start.jar

Other Platforms:

java -jar nntprss-start.jar

This will start the nntp//rss server.  The first time the server is
run, it will create and prepopulate a database with some sample feeds.
These feeds can be changed through the web-based administration
interface.

Note to Unix, Linux and Mac OS X users:

As nntp//rss starts an NNTP client listener on TCP port 119, 
if you are running on a Unix-based environment, the user under which
nntp//rss runs will need to have the appropriate rights to open that port.
If the user does not have those rights, you will see the following exception 
during startup:

19:38:44,527 [main] INFO  Main - Starting nntp//rss v0.2
java.net.BindException: Permission denied
        at java.net.PlainSocketImpl.socketBind(Native Method)
        at java.net.PlainSocketImpl.bind(PlainSocketImpl.java:331)
...


Accessing nntp//rss from your existing news reader
==================================================

The configuration process varies from one news reader to another, so
check the documentation for your news reader on this process.  

When configuring a new news server within your newsreader for nntp//rss,
specify the following as the news server's address:

127.0.0.1

e.g. In Outlook Express, you'll specify 127.0.0.1 as the value in
the News (NNTP) Server field.

Once you have configured the news server, you will be able to
download a list of the currently configured groups.

If you experience problems with a particular news reader, please
send an email, containing details about your Operating System,
news reader version, and the problem itself to nntprss@methodize.org


Administration of nntp//rss
===========================

nntp//rss has a Web-based administration interface.  Point your web
browser to the following location:

http://127.0.0.1:7810/

A list of the currently configured channels will be displayed.  From 
here you will be able add, delete, and check the status of existing
channels.  

System Configuration

Here you can alter the platform-wide polling interval.  
This defines how often nntp//rss will go out and check
RSS feeds for updates.  

In addition, you can export your current channel configuration,
or import a previous configuration.

The final option in System Configuration defines how content
is served to newsreaders.  You can either select multipart/alternative
(plain text and html), text/plain or text/html.

Click on help within the web interface for more information.


Shutting down nntp//rss
=======================

Windows - Right click on the nntp//rss System Tray icon, and 
select Exit.  Confirm the shutdown by click on the OK button.
The application will terminate all activity and cleanly shutdown.
If nntp//rss has been configured to run as a Windows Service,
use the stop button within Windows' Service Manager to shutdown
nntp//rss.

Unix/Linux - If nntp//rss is running as a foreground process in a
shell, issue a CTRL+C to cleanly shutdown the application.  If it
is running as a background process, send a SIGTERM to the java
process that is running nntp//rss.


Securing access to the web interface
====================================

Access to the web admin interface can be password protected 
by creating a file named users.properties in the root directory of 
your nntp//rss installation.

This file takes the form:

username:password

e.g. sample user.properties

jason:mypassword

Multiple users can be configured by adding additional lines of
usernames and passwords in the above format.

You will need to stop and restart nntp//rss for these changes
to take effect.  The next time you access the web interface, you
will be prompted to enter your user name and password.

To disable secure access, just delete or rename the users.properties
file, and stop and restart nntp//rss.


Running nntp//rss as a Windows Service
======================================

See the separate WINDOWS-SERVICE.TXT documentation for  
information on how to configure nntp//rss to run as a 
Windows service.


nntp//rss News Feed
===================

nntp//rss has a news feed containing the latest information 
about the project:

http://rss.methodize.org/nntprss.xml


Support
=======

If you have any questions, please email them to the following address:

nntprss@methodize.org

Please keep a close watch on the nntp//rss website for future updates.
