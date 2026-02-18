FROM alpine:3.23
RUN apk update && apk add openjdk21-jdk && apk add --no-cache maven
COPY HelloService /HelloService
RUN mvn -f /HelloService/pom.xml package
ENTRYPOINT ["java", "-jar", "/HelloService/target/HelloService.jar"]