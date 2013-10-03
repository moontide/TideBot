botdir=${botdir-.}
libdir=$botdir/../lib
dbdir=$botdir/../db
java -cp $libdir/commons-lang3-3.1.jar:$libdir/commons-exec-1.1.jar:$libdir/pircbot.jar:$libdir/jackson-core-2.2.3.jar:$libdir/jackson-databind-2.2.3.jar:$libdir/jackson-annotations-2.2.3.jar:$libdir/maxminddb-0.2.0.jar:$libdir/geoip2-0.4.1.jar:$botdir net.maclife.irc.LiuYanBot -geoipdb $dbdir/GeoLite2-City.mmdb -s irc.freenode.net -u FedoraBot -c "#linuxba,#LiuYanBot,fedora-zh"
# 2>&1
