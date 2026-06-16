FROM eclipse-temurin:26-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -q -pl interview-service -am clean package -DskipTests

FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /workspace/interview-service/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
