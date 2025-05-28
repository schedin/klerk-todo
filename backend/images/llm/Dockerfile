FROM docker.io/ollama/ollama:latest

ARG MODELS="qwen3:0.6b"
RUN ollama serve & server=$! ; sleep 5 ; for m in $MODELS ; do ollama pull $m ; done ; kill $server

EXPOSE 11434
