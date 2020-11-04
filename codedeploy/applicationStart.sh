#!/bin/bash

sudo chown -R ubuntu:ubuntu /home/ubuntu/*
cd /home/ubuntu/myApp/

sudo chmod +x ROOT.jar

source /etc/profile.d/envvariable.sh

nohup java -jar ROOT.jar > /home/ubuntu/applog.txt 2> /home/ubuntu/applog.txt < /home/ubuntu/applog.txt &