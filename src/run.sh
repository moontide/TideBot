botdir=${botdir:-$(dirname $0)}
libdir=$(readlink -e "$botdir/../lib")
dbdir=$(readlink -e "$botdir/../db")

OS=$(uname -s)
if [[ $OS == CYGWIN* ]]
then
	botdir=$(cygpath -w "$botdir")
	#libdir=$(cygpath -w "$libdir")
	#dbdir=$(cygpath -w "$dbdir")
	PATH_SEPARATOR=";"
elif [[ $OS == linux* ]]
then
	PATH_SEPARATOR=":"
else
	echo "未知操作系统: $OS，那么假定其路径分隔符是 ':'"
	PATH_SEPARATOR=":"
fi

echo "botdir = $botdir"
echo "libdir = $libdir"
echo "dbdir  = $dbdir"
echo "PATH   = $PATH"

server=${server:-chat.freenode.net}
#port=${port:-6667}
channels=${channels:-LiuYanBot,linuxba,fedora-zh}

nick=${nick:-CmdBot}
#account=${account:-liuyan}
#password=${password:-liuyan}

#-Djavax.net.debug=ssl,handshake,record -Ddeployment.security.TLSv1.2=false
export _JAVA_OPTIONS="-Dbotcmd.prefix=$botcmdPrefix -Dmessage.delay=500 -Djava.util.logging.config.file=$botdir/logging.properties -DGFWProxy.TrustStore=$dbdir/bot.jks -DGFWProxy.Type=${GFWProxyType} -DGFWProxy.Host=${GFWProxyHost-192.168.2.1} -DGFWProxy.Port=${GFWProxyPort-8087} -Ddatabase.driver=${database_driver-com.mysql.jdbc.Driver} -Ddatabase.username=${database_username-bot} -Ddatabase.userpassword=${database_userpassword} -Ddatabase.url=${database_url-jdbc:mysql://192.168.2.1/bot?autoReconnect=true&amp;zeroDateTimeBehavior=convertToNull} $_JAVA_OPTIONS"

for lf in commons-lang3-3.4 commons-io-2.5 commons-exec-1.3 commons-logging-1.2 commons-pool2-2.4.1 commons-dbcp2-2.0.1 commons-codec-1.10 \
    mysql-connector-java-5.1.40-bin  mariadb-java-client-1.5.2 \
    jackson-core-2.8.3  jackson-databind-2.8.3  jackson-annotations-2.8.3 \
    maxmind-db-1.2.1  geoip2-2.8.0  \
    google-pagerank-api-2.0 \
    jsoup-1.9.2 \
    bsh-2.0b4 \
    hcicloud-5.0.0 \
    QQWry StackExchangeAPI pircbot-mod
do
	if [[ $OS == CYGWIN* ]]
	then
		CP=${CP}$(cygpath -w "$libdir/$lf.jar")$PATH_SEPARATOR
	else
		CP=${CP}$libdir/$lf.jar$PATH_SEPARATOR
	fi
done

CP="${CP}$botdir"

echo "classpath=$CP"
export CLASSPATH="$CP"

java -cp "$CP" net.maclife.irc.LiuYanBot \
	-geoipdb $dbdir/GeoLite2-City.mmdb \
	-chunzhenipdb $dbdir/qqwry.dat \
	-oui $dbdir/oui.txt \
	-s $server \
	${port:+/port $port} \
	-n $nick \
	${account:+/u $account} \
	${password:+/p $password} \
	-c "$channels" \
	/ban "$ban" \
