#!/bin/sh
java -jar jetty-runner.jar --port 9090 --stop-port 9093 --stop-key oaistop oai.war
echo "Open your browser and go to http://localhost:9090/"

