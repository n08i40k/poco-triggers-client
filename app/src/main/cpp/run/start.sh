#!/bin/bash

adb shell "pkill aosp-triggers-daemon"
adb shell "rm /data/local/tmp/aosp-triggers-daemon*"
adb push aosp-triggers-daemon /data/local/tmp/
adb shell "chmod +x /data/local/tmp/aosp-triggers-daemon"
adb shell "/data/local/tmp/aosp-triggers-daemon"