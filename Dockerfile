FROM openjdk:8-alpine

COPY target/uberjar/harpocrates.jar /harpocrates/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/harpocrates/app.jar"]
