nntp//rss - version 0.1-prealpha
--------------------------------

December 29, 2002

Bridging the worlds of NNTP clients and RSS feeds, nntp//rss is an 
application that will enable you to use your existing favorite NNTP 
newsreader to read your information channels.

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


Accessing nntp//rss from your existing news reader
==================================================

The configuration process varies from one news reader to another,
but all you need to do is configure another news server, and specify
the following as its address:

127.0.0.1

e.g. In Outlook Express, you'll specify 127.0.0.1 as the value in
the News (NNTP) Server field.

Once you have configured the news server, you should be able to
download a list of the currently configured groups.


Administration of nntp//rss
===========================

nntp//rss has a Web-based administration interface.  Point your web
browser to the following location:

http://localhost:7810/

A list of the currently configured channels will be displayed.  From 
here you will be able add, delete, and check the status of existing
channels.  In addition, you can alter the platform-wide polling 
interval.  This defines how often nntp//rss will go out and check
RSS feeds for updates.



Support
=======

If you have any questions, please email them to the following address:

nntprss@methodize.org

Please keep a close watch on the nntp//rss website for future updates.
