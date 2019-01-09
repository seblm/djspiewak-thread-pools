#!/usr/bin/env bash

              curl --silent localhost:8080?1 &
sleep 0.02 && curl --silent localhost:8080?2 &
sleep 0.04 && curl --silent localhost:8080?3 &
sleep 0.06 && curl --silent localhost:8080?4 &
sleep 0.08 && curl --silent localhost:8080?5 &
sleep 0.10 && curl --silent localhost:8080?6 &
sleep 0.12 && curl --silent localhost:8080?7 &
sleep 0.14 && curl --silent localhost:8080?8 &
sleep 0.16 && curl --silent localhost:8080?9 &
sleep 0.18 && curl --silent localhost:8080?10 &
sleep 0.20 && curl --silent localhost:8080?11 &
sleep 5    && curl --silent localhost:8080/generate
