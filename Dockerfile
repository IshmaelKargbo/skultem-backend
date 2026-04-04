FROM maven:3.9.5-eclipse-temurin-21-alpine
WORKDIR /app

COPY pom.xml .
RUN mkdir -p src && echo "" > src/Main.java
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -B

CMD ["java", "-jar", "target/skultem-0.0.1-SNAPSHOT.jar"]