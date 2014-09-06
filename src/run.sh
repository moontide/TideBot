botdir=${botdir-.}
libdir=$(readlink -e "$botdir/../lib")
dbdir=$(readlink -e "$botdir/../db")
nick=${nick-CmdBot}
channels=${channels-LiuYanBot,linuxba,fedora-zh}

export _JAVA_OPTIONS="-Dbotcmd.prefix=$botcmdPrefix -Djava.util.logging.config.file=$botdir/logging.properties -Djavax.net.ssl.trustStore=$dbdir/GoAgentCA.jks -DGoAgent.proxyHost=${GoAgent_proxyHost-192.168.2.1} -DGoAgent.proxyPort=${GoAgent_proxyPort-8087} -Ddatabase.driver=${database_driver-com.mysql.jdbc.Driver} -Ddatabase.username=${database_username-bot} -Ddatabase.userpassword=${database_userpassword} -Ddatabase.url=${database_url-jdbc:mysql://192.168.2.1/bot?autoReconnect=true&amp;characterEncoding=UTF-8&amp;zeroDateTimeBehavior=convertToNull} $_JAVA_OPTIONS"

for lf in commons-lang3-3.3.2 commons-io-2.4 commons-exec-1.1 commons-logging-1.2 commons-pool2-2.2 commons-dbcp2-2.0.1 commons-codec-1.9 \
    mysql-connector-java-5.1.32-bin  mariadb-java-client-1.1.7 \
    jackson-core-2.4.2  jackson-databind-2.4.2  jackson-annotations-2.4.2 \
    maxmind-db-0.3.3  geoip2-0.8.0  \
    google-pagerank-api-2.0 \
    jsoup-1.7.3 \
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
	-s chat.freenode.net \
	-u $nick \
	-c "$channels" \
	/ban "$ban" \

#	/ban "*!*@59.78.22.*:熊孩D;*!*@unaffiliated/riaqn:穿了马甲的熊孩D" \
#-Djavax.net.ssl.trustStorePassword=changeit \

# 2>&1
