#export botdir=/home/bot/ircbot/src
export botdir=${botdir-$(dirname $0)}

export channels=${channels-LiuYanBot,debian-cn,linuxba}

export nick=DebCmdBot

export botcmdPrefix=deb

export LANG=zh_CN.UTF-8

$botdir/run.sh
