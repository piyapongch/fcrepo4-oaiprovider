@echo off
java -jar jetty-runner.jar --port 9090 --stop-port 9093 --stop-key oaistop oai.war

