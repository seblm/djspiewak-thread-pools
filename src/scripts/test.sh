#!/usr/bin/env bash

              curl --silent localhost:8080?1 &
sleep 0.01 && curl --silent localhost:8080?2 &
sleep 0.02 && curl --silent localhost:8080?3 &
sleep 1    && curl --silent localhost:8080/generate
