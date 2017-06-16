# Configuration for deploy the seed jee7 application in a docker container setup
#   - using port 8080 as cargo port and exposing that to dicker host port 8080
# ------------------------------------------------------------------------------------------------
# build the image as:      docker image build . -t seed-rest-server-jee7
# start the container as:  docker container run -p 8080:8080 seed-rest-server-jee7
# or in the background as: docker container run -d -p 8080:8080 seed-rest-server-jee7
# ------------------------------------------------------------------------------------------------
FROM jboss/wildfly:latest
ADD rest-services/target/sample.war /opt/jboss/wildfly/standalone/deployments/sample.war
ADD src/test/h2-data/account.h2.db src/test/h2-data/customer.h2.db /opt/jboss/wildfly/standalone/
ADD deploy-config/standalone.xml /opt/jboss/wildfly/standalone/configuration
EXPOSE 8080
WORKDIR /opt/jboss/wildfly/standalone