#------ Package Only Runtime Targets
#---------Service (API)
FROM git.genesis-platform.io:4567/engineering/docker-images/amazoncorretto:21.0.2-alpine3.19 as package-service

VOLUME /tmp

COPY ./build/libs/spring-graphql-union-with-pagination-api.jar app.jar

RUN apk update && apk add --no-cache curl gcompat

ENV JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,address=5025,server=y,suspend=n"
ENV SPRING_PROFILE="local"

ENTRYPOINT exec java $JAVA_OPTS \
 -Dspring.profiles.active=$SPRING_PROFILE \
 -jar app.jar

EXPOSE 8086
EXPOSE 5025

HEALTHCHECK --start-period=3m \
CMD curl -f http://localhost:8086/actuator/health || exit 1
