echo -e '\e[31;1m禁用掉一些命令，以较安全的方式执行\e[m'
#export botdir=${botdir-/home/liuyan/ircbot/src}
export botdir=$(dirname $0)
export nick=HtmlBot-_-
export botcmdPrefix=
export channels=LiuYanBot,linuxba
export ban="*:cmd:怕被玩坏，禁止所有人执行 Cmd 命令;*:javascript"
export LANG=zh_CN.UTF-8

#export database_driver=com.mysql.jdbc.Driver
export database_username=bot
#export database_passwrd=
export database_url="jdbc:mysql://localhost/bot?autoReconnect=true&amp;characterEncoding=UTF-8&amp;zeroDateTimeBehavior=convertToNull"

export GoAgent_proxyHost=localhost
export GoAgent_proxyPort=8087

$botdir/run.sh
