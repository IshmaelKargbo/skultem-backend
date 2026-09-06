FROM maven:3.9.5-eclipse-temurin-21-alpine
WORKDIR /app

COPY pom.xml .
RUN mkdir -p src && echo "" > src/Main.java
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -B

# Render's smallest instance is 512Mi. The default GC (G1) and unbounded metaspace/thread
# stacks push total JVM footprint well past that during boot (Hibernate building the metamodel
# for 80+ JPA repositories, Tomcat, the STOMP broker, Jackson autoconfig, etc.) even though -Xmx
# alone looks fine - that's what was OOM-killing the container before Tomcat could even bind its
# port. JAVA_TOOL_OPTIONS is read directly by the java launcher itself (no shell wrapper needed),
# and an ENV set here is just the image's default - Render setting its own JAVA_TOOL_OPTIONS env
# var still overrides it.
#
# MaxMetaspaceSize was previously 100m, which was too tight for this app's class metadata
# (Hibernate + Jackson + WebSocket autoconfig all load heavily during boot) and OOM'd there
# specifically instead of the container getting killed outright. Metaspace gets more room here;
# Xmx gives some back to compensate so the combined ceiling (220 + 160 + 40 = 420Mi) still leaves
# headroom under 512Mi for thread stacks and native/direct memory the JVM doesn't count against
# these flags. If it OOMs again - metaspace or otherwise - that headroom is exhausted and the
# real fix is more RAM, not tighter caps.
ENV JAVA_TOOL_OPTIONS="-Xmx220m -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=40m -Xss512k -XX:+UseSerialGC"

CMD ["java", "-jar", "target/skultem-0.0.1-SNAPSHOT.jar"]