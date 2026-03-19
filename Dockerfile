FROM amazoncorretto:21-alpine

COPY /target/wee-*.jar /wee/app.jar

WORKDIR /wee

EXPOSE 9080
ENTRYPOINT ["java", "-jar", "app.jar"]
