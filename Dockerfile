FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

WORKDIR /app
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

USER appuser

ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["-jar","app.jar"]