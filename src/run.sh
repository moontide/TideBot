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
nick=${nick:-CmdBot}
#account=${account:-liuyan}
#password=${password:-liuyan}
channels=${channels:-LiuYanBot,linuxba}

#-Djavax.net.debug=ssl,handshake,record -Ddeployment.security.TLSv1.2=false
export _JAVA_OPTIONS="-Dbotcmd.prefix=$botcmdPrefix \
-Dmessage.delay=100 -Djava.util.logging.config.file=$botdir/logging.properties \
-DGFWProxy.TrustStore=$dbdir/bot.jks \
-DGFWProxy.Type=${GFWProxyType} \
-DGFWProxy.Host=${GFWProxyHost-192.168.181.70} \
-DGFWProxy.Port=${GFWProxyPort-9910} \
-Ddatabase.driver=${database_driver-com.mysql.jdbc.Driver} \
-Ddatabase.username=${database_username-bot} \
-Ddatabase.userpassword=${database_userpassword} \
-Ddatabase.url=${database_url-jdbc:mysql://192.168.181.70/bot?autoReconnect=true&zeroDateTimeBehavior=convertToNull&useSSL=false} \
-Dgame.sanguosha.allowed-channels=#LiuYanBot \
$_JAVA_OPTIONS"

for lf in commons-lang3-3.12.0 commons-text-1.9 commons-io-2.11.0 commons-exec-1.3 commons-logging-1.2 commons-pool2-2.11.1 commons-dbcp2-2.9.0 commons-codec-1.15 \
    mysql-connector-java-8.0.28  mariadb-java-client-2.7.3 \
    jackson-core-2.13.1  jackson-databind-2.13.1  jackson-annotations-2.13.1 \
    maxmind-db-2.0.0  geoip2-2.15.0  \
    google-pagerank-api-2.0 \
    jsoup-1.14.3 \
    bsh-2.0b6 \
    jython-standalone-2.7.2 \
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

java -cp "$CP" \
	-Dgame.wordle.words.dictionary.file=$dbdir/words_5letters.txt \
	net.maclife.irc.LiuYanBot \
	-geoipdb /usr/share/GeoIP/GeoLite2-City.mmdb \
	-chunzhenipdb $dbdir/qqwry.dat \
	-oui $dbdir/oui.txt \
	-s $server \
	${port:+/port $port} \
	-n $nick \
	${account:+/u $account} \
	${password:+/p $password} \
	-c "$channels" \
	-s $server \
	${port:+/port $port} \
	-n $nick \
	/u liuyan/liberachat \
	${password:+/p $password} \
	-c "$channels" \
	/ban "$ban" \
