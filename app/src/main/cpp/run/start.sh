#!/bin/bash

adb -d shell "pkill poco-triggers-daemon"
adb -d shell "rm /data/local/tmp/poco-triggers-daemon*"
adb -d push poco-triggers-daemon /data/local/tmp/
adb -d shell "chmod +x /data/local/tmp/poco-triggers-daemon"
adb -d shell "/data/local/tmp/poco-triggers-daemon"