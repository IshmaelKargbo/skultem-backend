FROM maven:3.9.5-eclipse-temurin-21-alpine
WORKDIR /app

COPY pom.xml .
RUN mkdir -p src && echo "" > src/Main.java
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -B

# Render's smallest instance is 512Mi. The default GC (G1) and unbounded metaspace/thread
# stacks push total JVM footprint well past that during boot (Hibernate building the metamodel
# for 80+ JPA repositories, Tomcat, the STOMP broker, etc.) even though -Xmx alone looks fine -
# that's what was OOM-killing the container before Tomcat could even bind its port. JAVA_TOOL_OPTIONS
# is read directly by the java launcher itself (no shell wrapper needed), and an ENV set here is
# just the image's default - Render setting its own JAVA_TOOL_OPTIONS env var still overrides it.
ENV JAVA_TOOL_OPTIONS="-Xmx300m -XX:MaxMetaspaceSize=100m -XX:ReservedCodeCacheSize=32m -Xss512k -XX:+UseSerialGC"

CMD ["java", "-jar", "target/skultem-0.0.1-SNAPSHOT.jar"]