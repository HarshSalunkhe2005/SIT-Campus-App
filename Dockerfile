# Builds the backend and bundles the frontend pages so one service serves both (same origin, no CORS needed).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY campusbackend/ campusbackend/
RUN cd campusbackend && chmod +x mvnw && ./mvnw -q -DskipTests package && cp target/campusbackend-*.jar /build/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home appuser && mkdir -p /app/uploads && chown -R appuser /app
COPY --from=build /build/app.jar app.jar
COPY src/main/resources /app/frontend
ENV FRONTEND_DIR=/app/frontend \
    UPLOAD_DIR=/app/uploads \
    JAVA_OPTS="-Xmx350m -XX:+UseSerialGC"
USER appuser
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
