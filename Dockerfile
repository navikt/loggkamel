FROM gcr.io/distroless/java21-debian13
COPY /target/loggtransport.jar app.jar
CMD ["app.jar"]