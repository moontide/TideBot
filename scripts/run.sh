#!/bin/bash

script_dir=$(dirname $0)
bin_dir=${bin_dir:-$script_dir/../bin}
libdir=$(readlink -e "$bin_dir/../lib")
dbdir=$(readlink -e "$bin_dir/../db")
botcmdPrefix=${botcmdPrefix:-}

OS=$(uname -s)

shopt -s nocasematch

if [[ $OS == CYGWIN* ]]
then
	bin_dir=$(cygpath -w "$bin_dir")
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

echo "bin_dir = $bin_dir"
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
-Dmessage.delay=100 -Djava.util.logging.config.file=$bin_dir/logging.properties \
-DGFWProxy.TrustStore=$dbdir/bot.jks \
-DGFWProxy.Type=${GFWProxyType} \
-DGFWProxy.Host=${GFWProxyHost-192.168.181.69} \
-DGFWProxy.Port=${GFWProxyPort-9999} \
-Ddatabase.driver=${database_driver-com.mysql.jdbc.Driver} \
-Ddatabase.username=${database_username-bot} \
-Ddatabase.userpassword=${database_userpassword} \
-Ddatabase.url=${database_url-jdbc:mysql://192.168.181.69/bot?autoReconnect=true&zeroDateTimeBehavior=convertToNull&useSSL=false} \
-Dgame.sanguosha.allowed-channels=#LiuYanBot \
$_JAVA_OPTIONS"

for lf in commons-lang3-3.12.0 commons-text-1.10.0 commons-io-2.11.0 commons-exec-1.3 commons-logging-1.2 commons-pool2-2.11.1 commons-dbcp2-2.9.0 commons-codec-1.15 \
    pdfbox-2.0.27 fontbox-2.0.27 \
    mysql-connector-j-8.0.32  mariadb-java-client-2.7.3 \
    jackson-core-2.14.2  jackson-databind-2.14.2  jackson-annotations-2.14.2 \
    maxmind-db-3.0.0  geoip2-4.0.0  \
    jsoup-1.15.4 \
    bsh-2.0b6 \
    rhino-engine-1.7.14 rhino-runtime-1.7.14 \
    asm-9.4 asm-commons-9.4 asm-util-9.4 bluez-dbus-osgi-0.1.4 jffi-1.3.10 jnr-a64asm-1.0.0 jnr-enxio-0.32.14 jnr-posix-3.1.16 jnr-x86asm-1.0.2 slf4j-jdk14-2.0.6 asm-analysis-9.4 asm-tree-9.4 bluez-dbus-0.1.4 dbus-java-3.3.2 jffi-1.3.10-native jnr-constants-0.10.4 jnr-ffi-2.2.13 jnr-unixsocket-0.38.19 slf4j-api-2.0.6 \
    jython-standalone-2.7.3 \
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

CP="${CP}$bin_dir"

echo "classpath=$CP"
export CLASSPATH="$CP"

java -cp "$CP" \
	-Dgame.wordle.words.dictionary.file=$dbdir/words_5letters.txt \
	-Dprimary-secondary-negotiation.keystore.file=$dbdir/primary-secondary-negotiation.ks \
	-Dprimary-secondary-negotiation.keystore.password=changeit \
	-Dprimary-secondary-negotiation.key.name=psn-rsa \
	-Dprimary-secondary-negotiation.key.password=changeit \
	-Dprimary-secondary-negotiation.signature.algorithm=SHA256withRSA \
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
	/ban "$ban" \


#	-s $server \
#	${port:+/port $port} \
#	-n $nick \
#	/u liuyan/liberachat \
#	${password:+/p $password} \
#	-c "$channels" \
