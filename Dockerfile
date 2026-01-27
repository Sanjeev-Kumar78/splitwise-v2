# ===================== BUILD STAGE =====================
FROM maven:4.0.0-rc-5-eclipse-temurin-21-alpine AS build

WORKDIR /build
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw clean package -DskipTests

# ===================== RUNTIME STAGE =====================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
