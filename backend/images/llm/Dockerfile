FROM docker.io/ollama/ollama:latest

ARG MODELS="gemma3:1b-it-qat"
RUN ollama serve & server=$! ; sleep 5 ; for m in $MODELS ; do ollama pull $m ; done ; kill $server
