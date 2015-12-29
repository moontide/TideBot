echo -e '\e[31;1m禁用掉一些命令，以较安全的方式执行\e[m'
#export server=${server-card.freenode.net}
export server=${server-weber.freenode.net}
#export botdir=${botdir-/home/liuyan/ircbot/src}
export botdir=$(dirname $0)
export nick=GameBot2
export botcmdPrefix=
export channels=LiuYanBot
export ban="*:cmd:怕被玩坏，禁止所有人执行 Cmd 命令;*:/javascript;*:/java"
export LANG=zh_CN.UTF-8

#export database_driver=com.mysql.jdbc.Driver
export database_username=bot
#export database_passwrd=
export database_url="jdbc:mysql://192.168.115.88/bot?autoReconnect=true&amp;characterEncoding=UTF-8&amp;zeroDateTimeBehavior=convertToNull"

#export GFWProxyType=http
#export GFWProxyHost=localhost
#export GFWProxyPort=8087

export GFWProxyType=socks
export GFWProxyHost=192.168.90.201
export GFWProxyPort=9999

$botdir/run.sh
