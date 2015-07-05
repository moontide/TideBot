#!/bin/bash

source /etc/profile.d/env.sh


# $ curl -I https://standards.ieee.org/develop/regauth/oui/oui.txt -k
# HTTP/1.1 302 Found
# Date: Thu, 11 Jun 2015 13:48:47 GMT
# Location: http://standards-oui.ieee.org/oui.txt
# Content-Type: text/html; charset=iso-8859-1

file=oui.txt
temp_file=/var/tmp/$file
file_url=http://standards-oui.ieee.org/oui.txt
echo "$file_url"

size_file=/var/tmp/oui-size.txt
old_size=$(cat "$size_file")
new_size=$(curl -I "$file_url" 2>&1 | grep -iE "Content-Length|Last-Modified")

echo "old_size = [$old_size]"
echo "new_size = [$new_size]"

if [ "$old_size" == "$new_size" ]; then
	echo "oui 文件没改变，不必下载"
else
	# 删除可能存在的旧文件
	/usr/bin/rm -f $temp_file

	# 下载
	wget -c --no-cache -O $temp_file "$file_url"

	if [ $? -eq 0 ]
	then
		echo -n "$new_size" > $size_file

		# 移动到当前目录
		/bin/mv -f $temp_file .

		chmod 666 $file
	fi
fi
