echo -e '\e[31;1m禁用掉一些命令，以较安全的方式执行\e[m'
# 目前服务器连这个服务器相对稳定，很少断线
#export server=${server-weber.freenode.net}
export server=${server-card.freenode.net}
export botdir=${botdir-$(dirname $0)}
export nick=GameBot
export botcmdPrefix=
export channels=LiuYanBot,linuxba
export ban="*:cmd:服务器上运行，怕被玩坏，禁止所有人使用 Cmd 命令;*:javascript;*:java"
export LANG=zh_CN.UTF-8
#export _JAVA_OPTIONS="-Dhttp.proxyHost=192.168.115.245 -Dhttp.proxyPort=9999 -Dhttps.proxyHost=192.168.115.245 -Dhttps.proxyPort=9999  -Dhttp.nonProxyHosts= -Dhttp.proxyUser= -Dhttp.proxyPassword="

#export database_driver=com.mysql.jdbc.Driver
export database_username=bot
#export database_passwrd=
export database_url="jdbc:mysql://localhost/bot?autoReconnect=true&amp;characterEncoding=UTF-8&amp;zeroDateTimeBehavior=convertToNull"

#export GFWProxyType=HTTP
#export GFWProxyHost=localhost
#export GFWProxyPort=8087

# GFWProxyType 要注意用大写，因为是要用 java.net.Proxy.Type 解析的，如果小写则会报错： java.lang.IllegalArgumentException: No enum constant java.net.Proxy.Type.socks
export GFWProxyType=SOCKS
export GFWProxyHost=192.168.90.201
export GFWProxyPort=9999

$botdir/run.sh
