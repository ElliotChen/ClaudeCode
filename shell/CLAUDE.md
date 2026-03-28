# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This directory contains shell utilities for managing Claude Code with alternative LLM providers and running a local llama-server instance.

- `claudeCodeModels.sh` — Source this file to switch Claude Code between LLM providers (Qwen, MiniMax, Kimi, local Llama)
- `llamaserver.sh` — Standalone script to start/stop/manage a local llama-server process

## Usage

### Provider Switching (claudeCodeModels.sh)

Must be **sourced**, not executed:

```bash
source claudeCodeModels.sh

qwen          # Start Claude Code using Alibaba Qwen3.5-plus
minimax       # Start Claude Code using MiniMax-M2.5
kimi          # Start Claude Code using Kimi-K2.5
llama         # Start Claude Code using local llama-server (localhost:7080)
cc_reset      # Unset all overrides and restore Anthropic defaults

# Test provider endpoints without launching Claude Code
qwen test
llama test
```

`testllm` runs four curl-based tests: `thinking`, `tool_call`, `anthropic_messages`, `multimodal`.

### llama-server Management (llamaserver.sh)

```bash
./llamaserver.sh start              # Start with default model (qwen3)
./llamaserver.sh start gptoss       # Start specific model
./llamaserver.sh stop               # Graceful stop (SIGKILL after 10s)
./llamaserver.sh restart [model]    # Stop then start, optionally switching model
./llamaserver.sh status             # Show PID and current model
./llamaserver.sh list               # Show available models
./llamaserver.sh logs               # tail -f today's log
```

## Architecture

### Provider Configuration Pattern

Each provider function in `claudeCodeModels.sh` sets four environment variables, then calls `afterEnvConfig()`:

| Variable | Purpose |
|---|---|
| `ANTHROPIC_BASE_URL` | Anthropic messages API endpoint |
| `LOCAL_OPENAI_API_BASE_URL` | OpenAI-compatible endpoint |
| `ANTHROPIC_AUTH_TOKEN` | Bearer token |
| `ANTHROPIC_MODEL` | Model identifier |

`afterEnvConfig()` checks the first argument: if `test`, runs `testllm`; otherwise launches `claude --dangerously-skip-permissions` with `ENABLE_LSP_TOOLS=1`.

### llama-server Model Registry

Models are registered in two associative arrays in `llamaserver.sh`:
- `MODEL_MAP` — maps friendly name → GGUF file path (relative to `MODEL_DIR`)
- `MMPROJ_MAP` — maps name → multimodal projection file (optional, for vision models)

To add a new model, add an entry to `MODEL_MAP` (and `MMPROJ_MAP` if it has vision support).

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `LLAMA_PORT` | `7080` | Port for llama-server (shared between both scripts) |
| `LLAMA_SERVER_BIN` | `llama-server` | Path to llama-server binary |
| `LLAMA_MODEL_DIR` | `$HOME/llm/gguf` | Directory containing GGUF model files |
| `LLAMA_MODEL_LOG_DIR` | `$HOME/llm/logs` | Log output directory |
| `LLAMA_HOST` | `127.0.0.1` | llama-server listen address |

## Dependencies

Runtime: `bash` 4.0+ (Homebrew), `curl`, `jq`, `pgrep`, `ps`, `kill`, `nohup`
