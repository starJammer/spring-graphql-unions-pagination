#!/bin/bash

echo "=============================================="
echo "===== New Relic and OpenTelemetry setup. ====="
echo "=============================================="

if [[ -z "${newrelic_directory}" && -z "${opentelemetry_directory}" ]]; then
    echo "New Relic and Opentelemetry directories not set."
    export environment="local"
elif [[ ! -z "${opentelemetry_directory}" ]]; then
    echo "OpenTelemetry setup continue."
    opentelemetry_download_url_base="https://vp-bitbucket-downloads.s3.amazonaws.com/farscape/devops/opentelemetry/"
    # if otel java agent version is not provided use 1.26.0 as default
    [ -z "$opentelemetry_java_agent_version" ] && opentelemetry_java_agent_version="1.28.0"
    opentelemetry_java_agent_jar_file="opentelemetry-javaagent.jar"
    opentelemetry_java_agent_jar_full_path="${opentelemetry_download_url_base}${opentelemetry_java_agent_version}/java-agent/${opentelemetry_java_agent_jar_file}"

    opentelemetry_agent_status_code=$(curl "${opentelemetry_java_agent_jar_full_path}" -w '%{http_code}\n' -o "${opentelemetry_java_agent_jar_file}")

    if [ "${opentelemetry_agent_status_code}" == "200" ]; then
      echo "Opentelemetry java agent (v.${opentelemetry_java_agent_version}) successfully downloaded!"
      mkdir -p ${opentelemetry_directory}
      mv "${opentelemetry_java_agent_jar_file}" "${opentelemetry_directory}${opentelemetry_java_agent_jar_file}"
    fi
else
    echo "New Relic setup continue."
    # Pull the New Relic Java Agent with specified version if environment variable is set
    if  [ -n "${new_relic_download_url_base}" ] && [ -n "${new_relic_java_agent_version}" ]; then
      new_relic_java_agent_path="/java-agent/newrelic.jar"
      new_relic_java_agent_config_path="/config/newrelic-v2.yml.template"

      newRelicJavaAgentJARFullPath="${new_relic_download_url_base}${new_relic_java_agent_version}${new_relic_java_agent_path}"
      newRelicJavaAgentConfigFullPath="${new_relic_download_url_base}${new_relic_java_agent_version}${new_relic_java_agent_config_path}"
      newRelicJavaYMLFile="newrelic-temp.yml"
      newRelicJavaJARFile="newrelic.jar"

      curl "${newRelicJavaAgentJARFullPath}" -o "${newrelic_directory}${newRelicJavaJARFile}"
      curl "${newRelicJavaAgentConfigFullPath}" -o "${newrelic_directory}${newRelicJavaYMLFile}"

      # Hydrate New Relic Java File
      envsubst < "${newrelic_directory}${newRelicJavaYMLFile}" | yq eval -P > "${newrelic_directory}newrelic.yml"
    fi
fi

echo "=============================================="
echo "The application will start in ${APP_SLEEP}s..." && sleep ${APP_SLEEP}
echo "=============================================="

### START: Additional configuration can be setup here ###
### END:   Additional configuration can be setup here ###

exec java ${JAVA_OPTS} -Dspring.profiles.active=${environment} -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom -cp /app/resources/:/app/classes/:/app/libs/* "com.example.graphql.unionpagination.Application"  "$@"