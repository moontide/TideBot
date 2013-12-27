botdir=${botdir-.}
libdir=$botdir/../lib
dbdir=$botdir/../db
nick=${nick-CmdBot}
channels=${channels-LiuYanBot,linuxba,fedora-zh}
export _JAVA_OPTIONS="-Djava.util.logging.config.file=$botdir/logging.properties"
export CLASSPATH="$libdir/commons-lang3-3.1.jar:$libdir/commons-io-2.4.jar:$libdir/commons-exec-1.1.jar:$libdir/pircbot.jar:$libdir/jackson-core-2.2.3.jar:$libdir/jackson-databind-2.2.3.jar:$libdir/jackson-annotations-2.2.3.jar:$libdir/maxminddb-0.2.0.jar:$libdir/geoip2-0.4.1.jar:$libdir/google-pagerank-api-2.0.jar:$botdir"
java -Dbotcmd.prefix=$botcmdPrefix net.maclife.irc.LiuYanBot -geoipdb $dbdir/GeoLite2-City.mmdb -s irc.freenode.net -u $nick -c "$channels"
# 2>&1
