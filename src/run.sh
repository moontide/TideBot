botdir=${botdir:-$(dirname $0)}
libdir=$(readlink -e "$botdir/../lib")
dbdir=$(readlink -e "$botdir/../db")
botcmdPrefix=${botcmdPrefix:-}

OS=$(uname -s)

shopt -s nocasematch

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
export _JAVA_OPTIONS="-Dbotcmd.prefix=$botcmdPrefix \
-Dmessage.delay=100 -Djava.util.logging.config.file=$botdir/logging.properties \
-DGFWProxy.TrustStore=$dbdir/bot.jks \
-DGFWProxy.Type=${GFWProxyType} \
-DGFWProxy.Host=${GFWProxyHost-192.168.2.1} \
-DGFWProxy.Port=${GFWProxyPort-8087} \
-Ddatabase.driver=${database_driver-com.mysql.jdbc.Driver} \
-Ddatabase.username=${database_username-bot} \
-Ddatabase.userpassword=${database_userpassword} \
-Ddatabase.url=${database_url-jdbc:mysql://192.168.2.1/bot?autoReconnect=true&amp;zeroDateTimeBehavior=convertToNull} \
-Dgame.sanguosha.allowed-channels=#LiuYanBot\
$_JAVA_OPTIONS"

for lf in commons-lang3-3.8 commons-text-1.2 commons-io-2.6 commons-exec-1.3 commons-logging-1.2 commons-pool2-2.5.0 commons-dbcp2-2.2.0 commons-codec-1.11 \
    mysql-connector-java-5.1.48-bin  mariadb-java-client-2.0.2 \
    jackson-core-2.9.10  jackson-databind-2.9.10  jackson-annotations-2.9.10 \
    maxmind-db-1.2.2  geoip2-2.9.0  \
    google-pagerank-api-2.0 \
    jsoup-1.12.1 \
    bsh-2.0b6 \
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
