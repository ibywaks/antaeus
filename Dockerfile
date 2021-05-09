FROM adoptopenjdk/openjdk11:latest

RUN apt-get update && \
    apt-get install -y sqlite3

WORKDIR /antaeus
COPY . .

ENV MODE="staging"
ENV DB_FILE="db/db.sqlite"
EXPOSE 7000
# When the container starts: build, test and run the app.
CMD ./gradlew build && ./gradlew test && ./gradlew run

