FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (for caching)
COPY pom.xml .
RUN mkdir -p src && echo "" > src/Main.java
RUN mvn dependency:go-offline -B

# Copy source code
COPY . .

# Build the app
RUN mvn clean package -DskipTests

# Expose default port (Render uses $PORT)
ENV PORT=10000

EXPOSE $PORT

# Run the app
ENTRYPOINT ["sh", "-c", "java -Xmx128m -Xms128m -jar target/*.jar --server.port=$PORT"]