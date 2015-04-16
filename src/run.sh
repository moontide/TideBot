botdir=${botdir-$(dirname $0)}
libdir=$(readlink -e "$botdir/../lib")
dbdir=$(readlink -e "$botdir/../db")
nick=${nick-CmdBot}
channels=${channels-LiuYanBot,linuxba,fedora-zh}
server=${server-chat.freenode.net}

export _JAVA_OPTIONS="-Dbotcmd.prefix=$botcmdPrefix -Dmessage.delay=1000 -Djava.util.logging.config.file=$botdir/logging.properties -Djavax.net.ssl.trustStore=$dbdir/bot.jks -DGoAgent.proxyHost=${GoAgent_proxyHost-192.168.2.1} -DGoAgent.proxyPort=${GoAgent_proxyPort-8087} -Ddatabase.driver=${database_driver-com.mysql.jdbc.Driver} -Ddatabase.username=${database_username-bot} -Ddatabase.userpassword=${database_userpassword} -Ddatabase.url=${database_url-jdbc:mysql://192.168.2.1/bot?autoReconnect=true&amp;characterEncoding=UTF-8&amp;zeroDateTimeBehavior=convertToNull} $_JAVA_OPTIONS"

for lf in commons-lang3-3.3.2 commons-io-2.4 commons-exec-1.3 commons-logging-1.2 commons-pool2-2.3 commons-dbcp2-2.0.1 commons-codec-1.9 \
    mysql-connector-java-5.1.35-bin  mariadb-java-client-1.1.8 \
    jackson-core-2.5.2  jackson-databind-2.5.2  jackson-annotations-2.5.2 \
    maxmind-db-1.0.0  geoip2-2.1.0  \
    google-pagerank-api-2.0 \
    jsoup-1.8.2 \
    QQWry StackExchangeAPI pircbot-mod
do
	CP="${CP}$libdir/$lf.jar:"
done

CP="${CP}$botdir"
echo "classpath=$CP"
export CLASSPATH="$CP"

java net.maclife.irc.LiuYanBot \
	-geoipdb $dbdir/GeoLite2-City.mmdb \
	-chunzhenipdb $dbdir/qqwry.dat \
	-s $server \
	-u $nick \
	-c "$channels" \
	/ban "$ban" \

#-Djavax.net.ssl.trustStorePassword=changeit \

# 2>&1
