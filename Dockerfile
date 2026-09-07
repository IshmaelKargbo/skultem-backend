FROM maven:3.9.5-eclipse-temurin-21-alpine

WORKDIR /app

# Copy Maven configuration first for better Docker layer caching
COPY pom.xml .

# Create a minimal source directory so dependency:go-offline can run
RUN mkdir -p src && echo "" > src/Main.java

RUN mvn dependency:go-offline -B

# Copy application source
COPY src ./src

# Build application
RUN mvn package -B -DskipTests

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=55 -XX:MaxMetaspaceSize=192m -Xss512k"

CMD ["java", "-jar", "target/skultem-0.0.1-SNAPSHOT.jar"]