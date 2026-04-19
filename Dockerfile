FROM eclipse-temurin:17-jdk AS build
RUN apt-get update && apt-get install -y ant
WORKDIR /app
COPY . .
RUN ant jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/dist/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
