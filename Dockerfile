FROM gcr.io/distroless/java21-debian13
COPY /target/loggkamel.jar app.jar
CMD ["app.jar"]