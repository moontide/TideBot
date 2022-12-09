#!/bin/bash

script_dir=$(dirname "$0")

app="completions"
prompt="$1"
model="${2:-text-davinci-003}"
post_data='{
  "model": "'"$model"'",
  "prompt": "'"$prompt"'",
  "temperature": 0.3,
  "max_tokens": 500,
  "top_p": 1.0,
  "frequency_penalty": 0.0,
  "presence_penalty": 0.0
}'

result_json=$("$script_dir/openai-common.sh" "$app" "$post_data")

echo "$result_json" 1>&2

echo "$result_json" | jq -j '.choices[0].text, "    [模型=", .model, "; 输入token数=", .usage.prompt_tokens, ", 补全token数=", .usage.completion_tokens, ", 总token数=", .usage.total_tokens, "]"'
