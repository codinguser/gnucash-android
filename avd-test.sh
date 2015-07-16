#!/bin/bash
set -e
function disable_animation() {
  echo "Disabling animations"
  echo "    Opening Developer Settings"
  adb shell am start -W -n com.android.settings/.DevelopmentSettings
  echo "    Scroll to Window animation scale"
  for i in {1..23}; do
    adb shell input keyevent 20
  done
  echo "    Set Window animation, Transition scale and Animator duration scale to 0x"
  for i in {1..3}; do
    adb shell input keyevent 20
    adb shell input keyevent 23
    adb shell input keyevent 19
    adb shell input keyevent 19
    adb shell input keyevent 23
  done
  adb shell input keyevent 3
  echo "Done"
}

if [ ! -d "$HOME/.android/avd/$NAME.avd" ] || [ ! -f "$HOME/.android/avd/$NAME.ini" ]; then 
  mkdir sdcard
  mksdcard -l gnucash-sdcard 64M sdcard/gnucash-sdcard.img
  echo "no" | android create avd --force -n "$NAME" -t "$TARGET" --abi armeabi-v7a 
  emulator -avd "$NAME" -no-skin -no-audio -no-boot-anim -no-window -sdcard sdcard/gnucash-sdcard.img &
  android-wait-for-emulator
  adb shell input keyevent 82
  disable_animation 
else
  echo "Using AVD cache"
  emulator -avd "$NAME" -no-audio -no-window -no-skin -no-boot-anim &
  android-wait-for-emulator
  adb shell input keyevent 82 &
fi