set botdir=%cd%
set libdir=%botdir%\..\lib
set dbdir=%botdir%\..\db
set nick=WinCmdBot
set channels=LiuYanBot,linuxba
set botcmdPrefix=Win

echo "botdir=%botdir%"
echo "libdir=%libdir%"
echo "dbdir=%dbdir%"
echo "nick=%nick%"
echo "channels=%channels%"

set _JAVA_OPTIONS="-Djava.util.logging.config.file=%botdir%\logging.properties"
set CLASSPATH=%libdir%\commons-lang3-3.4.jar;%libdir%\commons-io-2.5.jar;%libdir%\commons-exec-1.3.jar;%libdir%\commons-logging-1.2.jar;%libdir%\commons-pool2-2.4.2.jar;%libdir%\commons-dbcp2-2.1.1.jar;%libdir%\commons-codec-1.10.jar;%libdir%\mysql-connector-java-5.1.41-bin.jar;%libdir%\mariadb-java-client-1.5.9.jar;%libdir%\jackson-core-2.8.8.jar;%libdir%\jackson-databind-2.8.8.jar;%libdir%\jackson-annotations-2.8.8.jar;%libdir%\maxmind-db-1.2.2.jar;%libdir%\geoip2-2.8.1.jar;%libdir%\google-pagerank-api-2.0.jar;%libdir%\jsoup-1.10.2.jar;%libdir%\bsh-2.0b4.jar;%libdir%\QQWry.jar;%libdir%\StackExchangeAPI.jar;%libdir%\pircbot-mod.jar;%libdir%\hcicloud-5.0.0.jar;%botdir%

echo "classpath=%CLASSPATH%"

java -Dbotcmd.prefix=%botcmdPrefix% -Dmessage.delay=500 -Djava.util.logging.config.file=%botdir%\logging.properties -Djavax.net.ssl.trustStore=%dbdir%\bot.jks -DGFWProxy.Type= -DGFWProxy.Host= -DGFWProxy.Port= -Ddatabase.driver=com.mysql.jdbc.Driver -Ddatabase.username=bot -Ddatabase.userpassword= -Ddatabase.url="jdbc:mysql://192.168.115.88/bot?autoReconnect=true&zeroDateTimeBehavior=convertToNull" net.maclife.irc.LiuYanBot -geoipdb %dbdir%\GeoLite2-City.mmdb -chunzhenipdb %dbdir%\qqwry.dat -oui %dbdir%\oui.txt -s chat.freenode.net -n %nick% -c "%channels%"
