echo -e '\e[31;1m禁用掉一些命令，以较安全的方式执行\e[m'
#export botdir=${botdir:-/home/liuyan/ircbot/src}
export botdir=$(dirname $0)

#export server=${server:-card.freenode.net}
export server=${server:-weber.freenode.net}
export channels=liuyanbot

export nick=GameBot2

export botcmdPrefix=

export ban="*:cmd:怕被玩坏，禁止所有人执行 Cmd 命令;*:/javascript;*:/java"

export LANG=zh_CN.UTF-8

#export database_driver=com.mysql.jdbc.Driver
export database_username=bot
#export database_passwrd=
export database_url="jdbc:mysql://192.168.115.88/bot?autoReconnect=true&zeroDateTimeBehavior=convertToNull&useSSL=false"

#export GFWProxyType=HTTP
#export GFWProxyHost=localhost
#export GFWProxyPort=8087

export GFWProxyType=SOCKS
export GFWProxyHost=192.168.181.69
export GFWProxyPort=9909

$botdir/run.sh
