botdir=${botdir-.}
libdir=$botdir/../lib
dbdir=$botdir/../db
nick=${nick-CmdBot}
channels=${channels-LiuYanBot,linuxba,fedora-zh}

export _JAVA_OPTIONS="-Djava.util.logging.config.file=$botdir/logging.properties"
export CLASSPATH="$libdir/commons-lang3-3.3.2.jar:$libdir/commons-io-2.4.jar:$libdir/commons-exec-1.1.jar:$libdir/pircbot.jar:$libdir/jackson-core-2.4.1.jar:$libdir/jackson-databind-2.4.1.1.jar:$libdir/jackson-annotations-2.4.1.jar:$libdir/maxmind-db-0.3.3.jar:$libdir/geoip2-0.7.2.jar:$libdir/google-pagerank-api-2.0.jar:$libdir/QQWry.jar:$libdir/StackExchangeAPI.jar:$botdir"

java -Dbotcmd.prefix=$botcmdPrefix \
	-Djavax.net.ssl.trustStore=$dbdir/GoAgentCA.jks \
	net.maclife.irc.LiuYanBot \
	-geoipdb $dbdir/GeoLite2-City.mmdb \
	-chunzhenipdb $dbdir/qqwry.dat \
	-s chat.freenode.net \
	-u $nick \
	-c "$channels" \

#	/ban "*!*@59.78.22.*:熊孩D;*!*@unaffiliated/riaqn:穿了马甲的熊孩D" \
#-Djavax.net.ssl.trustStorePassword=changeit \

# 2>&1
