# Build: JDK 25 με Maven Wrapper. Οι έλεγχοι τρέχουν στο CI πριν από το build του image.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
# Το .git αντιγράφεται για να γραφτεί το commit στο /actuator/info.
COPY . .
RUN chmod +x mvnw && ./mvnw -B -ntp package -DskipTests

# Εκτέλεση: μόνο JRE 25, χωρίς δικαιώματα root.
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 1001 --no-create-home escapehub
COPY --from=build /workspace/target/escapehub-*.jar app.jar
USER escapehub
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
