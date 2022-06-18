# syntax=docker/dockerfile:1
FROM openjdk:17
WORKDIR .
COPY target/shinsetsu-standalone.jar .
COPY config.edn .
CMD ["java", "-Dconf=config.edn", "-jar", "shinsetsu-standalone.jar"]
