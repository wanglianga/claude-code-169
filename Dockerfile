# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/binroom-linkage.jar /app/app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=kafka
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
