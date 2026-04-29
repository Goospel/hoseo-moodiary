FROM ubuntu:latest
LABEL authors="kimsa"

ENTRYPOINT ["top", "-b"]