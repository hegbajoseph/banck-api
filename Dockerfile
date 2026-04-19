FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN apt-get update && apt-get install -y ant
RUN ant -f build.xml jar
EXPOSE 8080
CMD ["java", "-jar", "dist/banck-api.jar"]
