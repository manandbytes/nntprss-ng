nntp//rss - version 0.1
-----------------------

December 30, 2002

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
  Win32, and JDK 1.4.1 on Solaris and Red Hat Linux 7.2)
  Sun's Java Runtime Environment may be downloaded from:
  http://java.sun.com/j2se/downloads.html
  
- NNTP-based news reader
  (nntp//rss has been tested with Outlook Express 6,
  Netscape v4.7 and Free Agent 1.92)


Installation
============

To install nntp//rss, simply unpack the distribution into a directory
of your choice.  No further configuration is necessary at this stage.


Running nntp//rss
=================

In the directory where you unpacked the nntp//rss distribution, execute
the following command:

java -jar nntprss-start.jar

This will start the nntp//rss server.  The first time the server is
run, it will create and prepopulate a database with some sample feeds.
These feeds can be changed through the web-based administration
interface.

Note to Unix/Linux users:

As nntp//rss starts an NNTP client listener on TCP port 119, 
if you are running on a Unix-based environment, the user under which
nntp//rss runs will need to have the appropriate rights to open that port.
If the user does not have those rights, you will see the following exception 
during startup:

19:38:44,527 [main] INFO  Main - Starting nntp//rss v0.1
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

System Configuration - Here you can alter the platform-wide polling 
interval.  This defines how often nntp//rss will go out and check
RSS feeds for updates.  In addition, you can export your current
channel configuration, or import a previous configuration.


Shutting down nntp//rss
=======================

Windows - Issue a CTRL+C in the Window where nntp//rss is running.
The application will terminate all activity and cleanly shutdown

Unix/Linux - If nntp//rss is running as a foreground process in a
shell, issue a CTRL+C to cleanly shutdown the application.  If it
is running as a background process, send a SIGTERM to the java
process that is running nntp//rss.


Running nntp//rss as a Windows Service
======================================

Check out http://www.eworksmart.com/JNT/ for a Windows Service 
wrapper for Java applications.  This has yet to be tested with
nntp//rss, however has proven reliable with other applications.
In a future release a separate HOW-TO will be published detailing 
how to configure JNT to run nntp//rss as a service.


Support
=======

If you have any questions, please email them to the following address:

nntprss@methodize.org

Please keep a close watch on the nntp//rss website for future updates.
