#! /bin/sh
java -Xms1024M -classpath "lib/*:build/*" -Dlog4j.configurationFile=config/log4j2-master.xml org.drftpd.master.Master
