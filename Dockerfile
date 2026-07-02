FROM node:22-alpine AS frontend-build
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-21 AS backend-build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
COPY --from=frontend-build /build/frontend/dist ./src/main/resources/static
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV SERVER_PORT=8080
COPY --from=backend-build /build/target/*.jar /app/sub2-monitor.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/sub2-monitor.jar"]
