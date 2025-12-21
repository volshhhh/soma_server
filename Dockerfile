FROM eclipse-temurin:17-jdk-alpine as builder

WORKDIR /app

# Копируем файлы сборки
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Скачиваем зависимости (кэшируется Docker)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Копируем исходный код и собираем
COPY src ./src
RUN ./mvnw package -DskipTests -B

# =============================================================================
# Финальный образ
# =============================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Создаем пользователя для запуска приложения
RUN addgroup -g 1000 spring && adduser -u 1000 -G spring -s /bin/sh -D spring
USER spring:spring

# Копируем собранный JAR
COPY --from=builder /app/target/*.jar app.jar

# Порт приложения
EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Запуск с оптимальными JVM параметрами для контейнера
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
