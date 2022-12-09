#!/bin/bash

script_dir=$(dirname "$0")

app="models"
input="$1"

result_json=$("$script_dir/openai-common.sh" "$app")

echo "$result_json" 1>&2

echo "$result_json" | jq -j '.data[] | .id, " "'
