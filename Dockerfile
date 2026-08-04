# --- Build stage ---
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
RUN keytool -printcert -sslserver repo.maven.apache.org:443 -rfc > /tmp/repo-maven.pem \
    && keytool -importcert -noprompt -alias repo-maven \
    -file /tmp/repo-maven.pem -cacerts -storepass changeit
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
