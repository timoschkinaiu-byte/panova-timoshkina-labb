
FROM eclipse-temurin:17-jdk

# Установка переменных окружения
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Создание директории приложения
WORKDIR /app

# Копирование JAR файла
COPY target/labb_2-1.0-SNAPSHOT.jar app.jar

# Создание пользователя для безопасности
RUN groupadd -r spring && useradd -r -g spring spring
USER spring

# Открытие порта
EXPOSE 8080

# Запуск приложения
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]