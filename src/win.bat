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
set CLASSPATH=%libdir%\commons-lang3-3.3.2.jar;%libdir%\commons-io-2.4.jar;%libdir%\commons-exec-1.1.jar;%libdir%\pircbot.jar;%libdir%\jackson-core-2.4.1.jar;%libdir%\jackson-databind-2.4.1.1.jar;%libdir%\jackson-annotations-2.4.1.jar;%libdir%\maxmind-db-0.3.3.jar;%libdir%\geoip2-0.7.2.jar;%libdir%\google-pagerank-api-2.0.jar;%libdir%\QQWry.jar;%libdir%\StackExchangeAPI.jar;%botdir%

echo "classpath=%CLASSPATH%"

java -Dbotcmd.prefix=%botcmdPrefix% -Djavax.net.ssl.trustStore=%dbdir%\GoAgentCA.jks net.maclife.irc.LiuYanBot -geoipdb %dbdir%\GeoLite2-City.mmdb -chunzhenipdb %dbdir%\qqwry.dat -s chat.freenode.net -u %nick% -c "%channels%" 
