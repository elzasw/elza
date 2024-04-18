FROM tomcat:10-jdk17-temurin

RUN rm -rf /usr/local/tomcat/webapps
RUN mkdir /usr/local/tomcat/webapps

COPY elza.war /usr/local/tomcat/webapps/ROOT.war
