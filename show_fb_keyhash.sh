#!/bin/sh

keytool -exportcert -alias vidiom -keystore /Users/andycat/src/Vidiom-distrib/vidiom-keystore  | openssl sha1 -binary | openssl base64

