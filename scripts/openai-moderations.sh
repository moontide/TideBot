#!/bin/bash

script_dir=$(dirname "$0")

app="moderations"
input="$1"
post_data='{
  "input": "'"$input"'"
}'

result_json=$("$script_dir/openai-common.sh" "$app" "$post_data")

echo "$result_json" 1>&2

echo "$result_json" | jq -j '"类别：仇恨=", .results[0].categories.hate, "，仇恨/威胁=", .results[0].categories."hate/threatening", "，自残=", .results[0].categories."self-harm", "，性=", .results[0].categories.sexual, "，性/未成年人=", .results[0].categories."sexual/minors", "，暴力=", .results[0].categories.violence, "，暴力/图像=", .results[0].categories."violence/graphic", "。模型：",.model'
