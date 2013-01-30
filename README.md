Svelto Eclipse plugin
===================

Svelto is an lightweight monitoring tool for Eclipse. It constantly checks the UI thread
automatically takes a thread snapshot when the UI thread is unresponsive for a (configurable)
period of time.

# Installation

Point Eclipse to the followin update site:

http://download.scala-ide.org/plugins/svelto/releases/2.10.x/site

## Note

Svelto is written in Scala, and requires the Scala library plugin. If you already have the 
[Scala IDE](www.scala-ide.org) plugin (for 2.10), you should not have any problem running this plugin.
Otherwise, add the Scala IDE update site to Eclipse, and check `Contact all update sites to find required components`.

# Build

Run maven like this:

    mvn -P scala-2.10.0 clean install

# Hack

Eclipse project files are provided, so you can directly use `Import Existing projects inside Eclipse`.