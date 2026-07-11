# ---- Stage 1: build the fat jar ----
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app

# Copy pom first so dependency downloads cache as a layer
# and only re-run when pom.xml changes.
COPY pom.xml ./
RUN mvn dependency:go-offline --batch-mode

# Now copy source and build
COPY src/ ./src/
RUN mvn package --batch-mode -DskipTests

# ---- Stage 2: slim runtime image ----
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/feedwatcher.jar ./feedwatcher.jar

ENV JAVA_OPTS="-Xmx256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar feedwatcher.jar"]