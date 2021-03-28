FROM openjdk:8-alpine

COPY target/uberjar/shinsetsu.jar /shinsetsu/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/shinsetsu/app.jar"]
