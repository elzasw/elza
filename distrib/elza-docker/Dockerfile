FROM tomcat:8.5-jdk11-adoptopenjdk-hotspot

RUN rm -rf /usr/local/tomcat/webapps
RUN mkdir /usr/local/tomcat/webapps

COPY elza.war /usr/local/tomcat/webapps/ROOT.war
COPY elza.yaml /usr/local/tomcat/config/
