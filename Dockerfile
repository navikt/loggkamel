FROM gcr.io/distroless/java21-debian13
ENV TZ="Europe/Oslo"
COPY /target/loggkamel.jar app.jar
CMD ["app.jar"]