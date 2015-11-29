botdir=${botdir-$(dirname $0)}
libdir=$(readlink -e "$botdir/../lib")
dbdir=$(readlink -e "$botdir/../db")
nick=${nick-CmdBot}
channels=${channels-LiuYanBot,linuxba,fedora-zh}
server=${server-chat.freenode.net}

#-Djavax.net.debug=ssl,handshake,record -Ddeployment.security.TLSv1.2=false
export _JAVA_OPTIONS="-Dbotcmd.prefix=$botcmdPrefix -Dmessage.delay=1000 -Djava.util.logging.config.file=$botdir/logging.properties -DGFWProxy.TrustStore=$dbdir/bot.jks -DGFWProxy.Type=${GFWProxyType} -DGFWProxy.Host=${GFWProxyHost-192.168.2.1} -DGFWProxy.Port=${GFWProxyPort-8087} -Ddatabase.driver=${database_driver-com.mysql.jdbc.Driver} -Ddatabase.username=${database_username-bot} -Ddatabase.userpassword=${database_userpassword} -Ddatabase.url=${database_url-jdbc:mysql://192.168.2.1/bot?autoReconnect=true&amp;characterEncoding=UTF-8&amp;zeroDateTimeBehavior=convertToNull} $_JAVA_OPTIONS"

for lf in commons-lang3-3.4 commons-io-2.4 commons-exec-1.3 commons-logging-1.2 commons-pool2-2.4.1 commons-dbcp2-2.0.1 commons-codec-1.10 \
    mysql-connector-java-5.1.37-bin  mariadb-java-client-1.1.8 \
    jackson-core-2.6.3  jackson-databind-2.6.3  jackson-annotations-2.6.3 \
    maxmind-db-1.0.0  geoip2-2.3.1  \
    google-pagerank-api-2.0 \
    jsoup-1.8.3 \
    bsh-2.0b4 \
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
	-oui $dbdir/oui.txt \
	-s $server \
	-u $nick \
	-c "$channels" \
	/ban "$ban" \
