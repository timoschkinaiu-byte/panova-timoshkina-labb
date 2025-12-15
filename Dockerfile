FROM tomcat:9.0-jdk17

# Удаляем стандартные приложения Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

COPY ./target/labb_2-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war

# Открываем порт
EXPOSE 8080

# Запускаем Tomcat
CMD ["catalina.sh", "run"]