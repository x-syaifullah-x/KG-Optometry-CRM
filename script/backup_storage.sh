#!/usr/bin/env sh

#BUCKET=kgoptometrycrm.appspot.com
BUCKET=x-syaifullah-x.appspot.com

gsutil cp -r gs://$BUCKET .
zip -r storage_backup.zip $BUCKET
rm -rf $BUCKET