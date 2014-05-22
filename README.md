Svelto Eclipse plugin
=====================

Svelto is an lightweight monitoring tool for Eclipse. It constantly checks the UI thread
and automatically saves a thread dump when the UI thread is unresponsive for a (configurable)
period of time.

You can configure the amount of time before saving a thread dump, and the directory where to
save this file. The file is never erased, so you may want to delete it from time to time.

# Installation

Point Eclipse to the following update site:

	http://download.scala-ide.org/plugins/svelto/releases/2.10.x/site

or, for Scala 2.11:

	http://download.scala-ide.org/plugins/svelto/releases/2.11.x/site
	
> Svelto is written in Scala, and requires the Scala library plugin. If you already have the 
> [Scala IDE](www.scala-ide.org) plugin for 2.10, you should not have any problem running this plugin.
> Otherwise, add the [Scala IDE update](http://download.scala-ide.org/sdk/e37/scala210/dev/site/) site to Eclipse
> and check `Contact all update sites to find required components` on the Installation dialog.

# Build

Run maven like this:

    mvn -P scala-2.10.x clean install

# Hack

Eclipse project files are provided, so you can directly use `Import Existing projects inside Eclipse`.
