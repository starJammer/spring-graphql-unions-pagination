#-------- Download Dependencies
FROM git.genesis-platform.io:4567/engineering/docker-images/gradle:8.5.0-jdk21 as builder
ARG NEXUS_USERNAME
ARG NEXUS_PASSWORD

ENV NEXUS_USERNAME=${NEXUS_USERNAME}
ENV NEXUS_PASSWORD=${NEXUS_PASSWORD}
ENV GRADLE_USER_HOME=/home/gradle/src/.gradle

# For Debug
RUN apt-get update && apt-get install tree

WORKDIR /home/gradle/src

COPY --chown=gradle:gradle build.gradle.kts ./
COPY --chown=gradle:gradle settings.gradle.kts ./

RUN gradle build -x test

# Copy Source Code files
COPY --chown=gradle:gradle ./*.* ./

#------ Runtime
#---------Service (API)
FROM git.genesis-platform.io:4567/engineering/docker-images/amazoncorretto:21.0.2-alpine3.19 as service

VOLUME /tmp

COPY --from=builder /home/gradle/src/build/libs/spring-graphql-union-with-pagination-api.jar app.jar

RUN apk update && apk add --no-cache curl gcompat

ENV JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,address=5025,server=y,suspend=n"
ENV SPRING_PROFILE="docker"

ENTRYPOINT exec java $JAVA_OPTS \
 -Dspring.profiles.active=$SPRING_PROFILE \
 -jar app.jar

EXPOSE 8086
EXPOSE 5025

HEALTHCHECK --start-period=3m \
CMD curl -f http://localhost:8086/actuator/health || exit 1
