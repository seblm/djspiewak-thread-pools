#!/usr/bin/env bash

              curl --silent localhost:8080 &
sleep 0.04 && curl --silent localhost:8080 &
sleep 0.04 && curl --silent localhost:8080 &
sleep 1    && curl --silent localhost:8080/generate
