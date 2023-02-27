export script_dir=${script_dir:-$(dirname $0)}

export channels=${channels:-liuyanbot,linuxba}

export nick=DebCmdBot

export botcmdPrefix=deb

export LANG=zh_CN.UTF-8

$script_dir/run.sh
