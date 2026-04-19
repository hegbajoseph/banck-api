FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . .
RUN apt-get update && apt-get install -y ant && ant jar
EXPOSE 8080
CMD ["java", "-jar", "dist/banck-api.jar"]
