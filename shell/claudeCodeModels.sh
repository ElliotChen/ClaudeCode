
export ALI_CODING_PLAN_AR_URL="https://coding-intl.dashscope.aliyuncs.com/apps/anthropic"
export ALI_CODING_PLAN_OA_URL="https://coding-intl.dashscope.aliyuncs.com/v1"
export ALI_CODING_PLAN_KEY="sk-sp-"
export OPENAI_API_PORT=${LLAMA_PORT:-7080}

afterEnvConfig() {
	case "${1:-claude}" in
  		test)     testllm ;;
  		*)
  			export ENABLE_LSP_TOOLS=1
    		claude --dangerously-skip-permissions "$@"
    		;;
	esac
}


qwen() {
	export OPENAI_API_BASE_URL="${ALI_CODING_PLAN_OA_URL}"
	export ANTHROPIC_BASE_URL="${ALI_CODING_PLAN_AR_URL}"
	export ANTHROPIC_AUTH_TOKEN="${ALI_CODING_PLAN_KEY}"
	export ANTHROPIC_MODEL="qwen3.5-plus"
	afterEnvConfig "$@"
}


minimax() {
	export OPENAI_API_BASE_URL="${ALI_CODING_PLAN_OA_URL}"
	export ANTHROPIC_BASE_URL="${ALI_CODING_PLAN_AR_URL}"
	export ANTHROPIC_AUTH_TOKEN="${ALI_CODING_PLAN_KEY}"
	export ANTHROPIC_MODEL="MiniMax-M2.5"
	afterEnvConfig "$@"
}

kimi() {
	export OPENAI_API_BASE_URL="${ALI_CODING_PLAN_OA_URL}"
	export ANTHROPIC_BASE_URL="${ALI_CODING_PLAN_AR_URL}"
	export ANTHROPIC_AUTH_TOKEN="${ALI_CODING_PLAN_KEY}"
	export ANTHROPIC_MODEL="kimi-k2.5"
	afterEnvConfig "$@"
}


llama() {
	export OPENAI_API_BASE_URL="http://localhost:${OPENAI_API_PORT}/v1"
	export ANTHROPIC_BASE_URL="http://localhost:${OPENAI_API_PORT}"
	export ANTHROPIC_AUTH_TOKEN="sk-1234"
	export ANTHROPIC_MODEL="llama"
	afterEnvConfig "$@"
}

cc_reset() {
	unset OPENAI_API_BASE_URL
    unset ANTHROPIC_BASE_URL
    unset ANTHROPIC_AUTH_TOKEN
    unset ANTHROPIC_MODEL
    unset ANTHROPIC_DEFAULT_SONNET_MODEL
    unset ANTHROPIC_DEFAULT_OPUS_MODEL

    ## 定義在startClaudeCode中的環境變數
    unset ENABLE_LSP_TOOLS

    unset ALI_CODING_PLAN_AR_URL
    unset ALI_CODING_PLAN_KEY
    echo "已還原為 Anthropic 預設設定"
}

testllm() {
	thinking
	tool_call
	anthropic_messages
	multimodal
}


thinking() {
	endpoint=${OPENAI_API_BASE_URL}'/chat/completions'
	echo '#########################'
	echo 'invoke thinking model()';
	echo '#########################'
	curl ${endpoint} \
	-H 'Content-Type: application/json' \
	-H 'Authorization: Bearer '${ANTHROPIC_AUTH_TOKEN} \
	-d '{
        "model": "'${ANTHROPIC_MODEL}'",
        "messages": [
            {
                "role": "user",
                "content": "解釋為什麼 9.11 大於 9.9，請詳細說明你的思考過程"
            }
        ]
    }' | jq .
}


tool_call() {
	endpoint=${OPENAI_API_BASE_URL}'/chat/completions'
	echo '#########################'
	echo 'invoke tool_call()';
	echo '#########################'
	curl ${endpoint} \
	-H 'Content-Type: application/json' \
	-H 'Authorization: Bearer '${ANTHROPIC_AUTH_TOKEN} \
	-d '{
        "model": "'${ANTHROPIC_MODEL}'",
        "messages": [
            {
                "role": "user",
                "content": "What is the weather in Taipei?"
            }
        ],
        "tools": [
        	{
        		"type": "function",
        		"function": {
        			"name": "get_weather",
        			"description": "Get the current weather in a location",
        			"parameters": {
        				"type": "object",
        				"properties": {
        					"location": {
        						"type": "string",
        						"description": "City name"
        					}
        				},
        				"required": ["location"]
        			}
        		}
        	}
        ]
    }' | jq .
}

anthropic_messages() {
	endpoint=${ANTHROPIC_BASE_URL}'/v1/messages'
	echo '#########################'
	echo 'invoke anthropic_messages()';
	echo '#########################'
	curl ${endpoint} \
	-H 'Content-Type: application/json' \
	-H 'Authorization: Bearer '${ANTHROPIC_AUTH_TOKEN} \
	-d '{
        "model": "'${ANTHROPIC_MODEL}'",
        "stream": false,
        "max_tokens": 1024,
        "messages": [
        	{
                "role": "assistant",
                "content": [
        			{
          				"type": "text",
          				"text": "Your are the best assistant"
        			}
        		]
            },
            {
                "role": "user",
                "content": [
        			{
          				"type": "text",
          				"text": "Introduce your self."
        			}
        		]
            }
        ]
    }' | jq .
}

multimodal() {
	endpoint=${OPENAI_API_BASE_URL}'/chat/completions'
	echo '#########################'
	echo 'invoke multimodal()';
	echo '#########################'
	curl ${endpoint} \
	-H 'Content-Type: application/json' \
	-H 'Authorization: Bearer '${ANTHROPIC_AUTH_TOKEN} \
	-d '{
        "model": "'${ANTHROPIC_MODEL}'",
        "stream": false,
        "max_tokens": 1024,
        "messages": [
        	{
        		"role": "user",
        		"content": [
        			{"type": "text", "text": "Describe this image in detail."},
        			{"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,'/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAAUABQDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDi6KKK+ZP3EKKKKACiiigAooooA//Z'"}}
        		]
        	}
        ]
    }' | jq .
}