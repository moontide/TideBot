#export botdir=/home/bot/ircbot/src
export botdir=${botdir-$(dirname $0)}
$botdir/run.sh

