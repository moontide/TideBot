#!/bin/bash

script_dir=$(dirname "$0")
openai_api_key_file="$script_dir/../db/openai.apikey.txt"
openai_api_key=$(cat "$openai_api_key_file")

app="$1"
post_data="$2"

curl https://api.openai.com/v1/"$app" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $openai_api_key" \
  ${post_data:+-d "$post_data"}
