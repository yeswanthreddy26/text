#!/bin/bash

# Check if virtualenv is installed
if command -v virtualenv &> /dev/null; then
    echo "virtualenv is already installed."
else
    echo "Installing virtualenv......"
    sudo apt install -y python3-virtualenv
    sudo apt install virtualenv -y
    sudo apt install python3-venv -y
    sudo apt install python3-pip -y
    sudo apt install openjdk-17-jdk -y
    sudo apt install maven -y
    sudo apt install spring -y
    sudo apt install gradle -y
    sudo apt install nginx -y
    sudo apt install gunicorn -y
    sudo apt install supervisor -y
fi

    python3 -m venv yeswanth
    
if [ -d "yeswanth" ]
then
    echo "Python virtual environment exists."
else
    echo "Creating a virtual environment"
    python3 -m venv yeswanth
fi

echo "The current directory"
echo $PWD
echo -e "\n\n\n"

echo "Activating the virtual environment"
source yeswanth/bin/activate
echo -e "\n\n\n"

echo "Checking for logs"
if [ -d "logs" ]
then
    echo "Log folder exists."
else
    echo "Creating Logs"
    mkdir logs
    touch logs/error.log logs/access.log
fi

echo -e "\n\n\n"
echo "Giving Permission"
sudo chmod -R 777 logs
echo -e "\n\n\n"
echo "*********Script Ended************"
