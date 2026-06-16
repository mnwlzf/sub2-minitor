FROM maven:3.9-eclipse-temurin-21 AS app-build
WORKDIR /app
ARG VITE_BASE_PATH=/
ARG VITE_API_BASE_URL=/api
ENV VITE_BASE_PATH=${VITE_BASE_PATH}
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY web/package*.json web/
RUN mvn -B -Pwith-web dependency:go-offline
COPY src src
COPY web web
RUN mvn -B -Pwith-web package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=default
COPY --from=app-build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
