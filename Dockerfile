FROM maven:3.8-openjdk-8

WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# Environment defaults (override at runtime)
ENV HOST=host.docker.internal \
    PORT=1521 \
    SERVICE=ORCL \
    USERNAME=system \
    PASSWORD=oracle \
    BATCH_SIZE=1000 \
    RECORD_COUNT=100000 \
    ITERATIONS=3

CMD ["java", "-jar", "target/java-oracle-benchmark-1.0.0-SNAPSHOT.jar"]
