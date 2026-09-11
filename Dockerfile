FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25
ENV TZ="Europe/Oslo" \
    JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED"
COPY /target/loggkamel.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]