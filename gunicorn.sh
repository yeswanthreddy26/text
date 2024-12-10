#!/bin/bash

cd /var/lib/jenkins/workspace/mymart

$PWD

source yeswanth/bin/activate
pip3 install gunicorn

mvn package -DskipTests
java -jar target/MyMart-0.0.1-SNAPSHOT.jar

sudo rm /etc/systemd/system/gunicorn.service
sudo cp -rf gunicorn.socket /etc/systemd/system/
sudo cp -rf gunicorn.service /etc/systemd/system/

echo "$USER"
echo "$PWD"



sudo systemctl daemon-reload
sudo systemctl start gunicorn

echo "Gunicorn has started."

sudo systemctl enable gunicorn

echo "Gunicorn has been enabled."

sudo systemctl restart gunicorn


sudo systemctl status gunicorn
