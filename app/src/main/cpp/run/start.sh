#!/bin/bash

adb -d shell "pkill aosp-triggers-daemon"
adb -d shell "rm /data/local/tmp/aosp-triggers-daemon*"
adb -d push aosp-triggers-daemon /data/local/tmp/
adb -d shell "chmod +x /data/local/tmp/aosp-triggers-daemon"
adb -d shell "/data/local/tmp/aosp-triggers-daemon"