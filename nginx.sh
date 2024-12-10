#!/bin/bash

cd /var/lib/jenkins/workspace/mymart

sudo cp -rf nginx.conf /etc/nginx/sites-available/
#sudo rm /etc/nginx/sites-enabled/nginx.conf
#sudo ln -s /etc/nginx/sites-available/ /etc/nginx/sites-enabled/nginx.conf
chmod 710 /var/lib/jenkins/workspace/mymart 

#sudo sed -i 's/80 default_server/81 default_server/g' /etc/nginx/sites-enabled/default

sudo service nginx start

echo "Nginx has been started"

sudo service nginx status

java -jar target/MyMart-0.0.1-SNAPSHOT.jar
nohup java -jar target/MyMart-0.0.1-SNAPSHOT.jar > spring_boot.log 2>&1 &
jobs -l
ps -ef | grep "MyMart-0.0.1-SNAPSHOT.jar"
