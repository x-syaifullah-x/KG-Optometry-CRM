#!/usr/bin/env sh

BUCKET=kgoptometrycrm.appspot.com
FILE_NAME=storage_backup.zip

gsutil cp -r gs://$BUCKET .
zip -r $FILE_NAME $BUCKET
rm -rf $BUCKET