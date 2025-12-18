FROM tomcat:10.1-jdk17-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

COPY ServerUbicua.war /usr/local/tomcat/webapps/ROOT.war

COPY context.xml /usr/local/tomcat/conf/context.xml

