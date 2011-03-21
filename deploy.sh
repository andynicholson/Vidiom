#!/bin/sh

#Make a release
ant release

DATEAPK=`date +%y-%m-%d-%H-%M`
# copy latest binary into folder
cp bin/Vidiom-release.apk deployed/Vidiom-$DATEAPK.apk

# commit
git add deployed/Vidiom-$DATEAPK.apk
git commit -m "AUTO Committing binary APK " deployed/Vidiom-$DATEAPK.apk

#copy to a webserver for distribution
scp deployed/Vidiom-$DATEAPK.apk andy@vidiom.mobi:/var/www/vidiom.net.au/
