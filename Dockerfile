FROM node:22-alpine AS web-build
WORKDIR /app/web
COPY web/package*.json ./
RUN npm ci
COPY web/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS app-build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN mvn -B dependency:go-offline
COPY src src
COPY --from=web-build /app/web/dist src/main/resources/static
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=default
COPY --from=app-build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
