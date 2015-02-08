#!/bin/bash

source /etc/profile.d/env.sh

archive_file=qqwry.dat.zip
files_to_be_extract=qqwry.dat
page_url="http://www.newxing.com/Code/tool/QQwry.dat_608.html"
# 因为 newxing.com 网站的 qqwry.dat 每次在更新后其 url 会变化，所以，需要解析 html，取出最新的 qqwry.dat 的文件网址
qqwry_file_url=$(curl "$page_url" | iconv -f gbk | grep -oE '"/.*/qqwry.dat"')
qqwry_file_url=http://www.newxing.com${qqwry_file_url//\"/}
echo "$qqwry_file_url"

size_file=/var/tmp/qqwry-size.txt
old_size=$(cat $size_file)
echo "curl -I $qqwry_file_url"
new_size=`curl -I $qqwry_file_url 2>&1 | grep -E -i "Content-Length|Last-Modified"`

echo "old_size = [${old_size}]"
echo "new_size = [${new_size}]"

if [ "$old_size" == "$new_size" ]; then
	echo "qqwry 文件没改变，不必下载"
else
	# 删除可能存在的旧文件
	/usr/bin/rm -f $archive_file

	# 下载
	wget -c --no-cache -O $archive_file $qqwry_file_url

	if [ $? -eq 0 ]
	then
		echo -n "${new_size}" > $size_file

		# 解压缩
		#7za.exe e $archive_file ${parent_directory}${files_to_be_extract}
		unzip -o $archive_file ${files_to_be_extract}

		chmod 666 $files_to_be_extract
	fi
fi
