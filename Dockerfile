FROM ghcr.io/graalvm/jdk:22.2.0

COPY ./app.jar app.jar

CMD java -jar app.jar
