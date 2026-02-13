FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

COPY settings.xml /root/.m2/settings.xml

ARG SN_GITHUB_USERNAME
ARG SN_GITHUB_TOKEN

ENV SN_GITHUB_USERNAME=${SN_GITHUB_USERNAME}
ENV SN_GITHUB_TOKEN=${SN_GITHUB_TOKEN}

RUN ./mvnw -B -T 1C dependency:go-offline

COPY src src

RUN ./mvnw -B -T 1C clean package -DskipTests


FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
