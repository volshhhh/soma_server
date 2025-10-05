FROM eclipse-temurin:21.0.5_11-jre-jammy
WORKDIR /app
COPY server/target/*.jar app.jar
RUN groupadd -r spring && useradd -r -g spring spring
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]