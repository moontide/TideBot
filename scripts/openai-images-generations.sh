#!/bin/bash

script_dir=$(dirname "$0")

app="images/generations"
prompt="$1"
post_data='{
  "prompt": "'"$prompt"'",
  "n": 1,
  "size": "256x256"
}'

result_json=$("$script_dir/openai-common.sh" "$app" "$post_data")

echo "$result_json" 1>&2

echo "$result_json" | jq -r '.data[0].url'
