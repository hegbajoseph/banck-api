FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN apt-get update && apt-get install -y ant
RUN mkdir -p lib && \
    apt-get install -y wget && \
    wget -q https://jdbc.postgresql.org/download/postgresql-42.7.3.jar -O lib/postgresql-42.7.3.jar
RUN ant -f build.xml jar
COPY index.html index.html
COPY Bank-API-Integration_big-1.webp Bank-API-Integration_big-1.webp
EXPOSE 10000
ENV PORT=10000
CMD ["java", "-cp", "dist/banck-api.jar:lib/postgresql-42.7.3.jar", "banck.api.BanckApi"]