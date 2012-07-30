#!/bin/sh

keytool -exportcert -alias vidiom -keystore ./vidiom-next.keystore  | openssl sha1 -binary | openssl base64

