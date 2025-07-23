FROM alpine:latest

RUN apk update
RUN apk add curl
RUN apk add --no-cache grep
RUN apk add --no-cache bash
RUN apk add --no-cache tar
RUN apk update

RUN curl -sSL https://raw.githubusercontent.com/livekit/livekit/v1.8.4/install-livekit.sh | bash

COPY config.yaml config.yaml

EXPOSE 7880

ENTRYPOINT ["/bin/sh", "-c", "livekit-server --dev --bind 0.0.0.0 --config config.yaml > /var/log/livekit.log 2>&1"]

#docker build -t local/livekit:v1.8.4 .