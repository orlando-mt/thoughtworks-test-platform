# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# mvnw generado en Windows puede traer saltos de línea CRLF, que rompen en Linux
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Las pruebas corren en el pipeline antes de construir la imagen
RUN ./mvnw -B -q package -DskipTests

RUN java -Djarmode=tools -jar target/problem1-0.0.1-SNAPSHOT.jar \
      extract --layers --launcher --destination target/extracted

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app

COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

USER app
EXPOSE 8080
# La JVM usa hasta el 75% de la memoria del contenedor (en ECS: de los 1 GB de la tarea)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]