##
# Use Java base image and setup required DEBS as cacheable image.
##
ARG BASE_IMAGE="opennms/deploy-base:jre-2.0.6.b165"

FROM ${BASE_IMAGE} as sentinel-base

ADD --chown=10001:10001 ./tarball-root/ /usr/share/sentinel/

ARG REQUIRED_DEBS="hostname wget gettext openssh-client uuid-runtime"

ARG REPO_KEY_URL="https://debian.opennms.org/OPENNMS-GPG-KEY"

SHELL ["/bin/bash", "-c"]

# Prevent prompts
ENV DEBIAN_FRONTEND=noninteractive

# Collect generic steps in a layer for caching
# import keys & install required debs
RUN apt-get update && \
    apt-get --no-install-recommends install -yq gnupg ca-certificates && \
    curl -fsSL ${REPO_KEY_URL} | apt-key add && \
    apt-get --no-install-recommends install -yq ${REQUIRED_DEBS} && \
    apt-get -y autoclean && \
    apt-get -y clean && \
    rm -rf /var/cache/apt

# get any security updates for the base system
RUN grep security /etc/apt/sources.list > /etc/apt/security.sources.list && \
    apt update -o Dir::Etc::SourceList=/etc/apt/security.sources.list && \
    apt -y -u -o Dir::Etc::SourceList=/etc/apt/security.sources.list full-upgrade && \
    apt clean && \
    rm -rf /var/cache/apt /etc/apt/security.sources.list

##
# Install and setup OpenNMS Sentinel
##
FROM sentinel-base

SHELL ["/bin/bash", "-c"]

# Create Sentinel user with a specific group ID
RUN groupadd -g 10001 sentinel && \
    useradd \
      --system \
      --uid 10001 \
      --gid 10001 \
      --home-dir /usr/share/sentinel \
      --no-create-home \
      --shell /usr/bin/bash \
      sentinel

# we want these to break the caching so yum will re-install now that
# we're not copying DEBs into /tmp/debs by default
ARG BUILD_DATE="1970-01-01T00:00:00+0000"
ARG REVISION
ARG ADD_DEB_PACKAGES

RUN id sentinel
RUN  getent group sentinel
RUN  getent passwd sentinel

# Install any spare packages and create some compatibility links
RUN if [[ -n "${ADD_DEB_PACKAGES}" ]]; then \
        apt-get --no-install-recommends install -yq ${ADD_DEB_PACKAGES}; \
    fi && \
    install -d -g 10001 -o 10001 -m 755 \
        /var/lib/sentinel \
        /usr/share/sentinel/data/{log,tmp} && \
    ln -s /usr/share/sentinel/data /var/lib/sentinel/data && \
    ln -s /usr/share/sentinel/deploy /var/lib/sentinel/deploy && \
    ln -s /usr/share/sentinel/etc /etc/sentinel && \
    chown 10001:10001 /var/lib/sentinel/* && \
    ln -s /usr/share/sentinel /opt/sentinel

COPY ./assets/* /

# Allow to send ICMP messages as non-root user
RUN setcap cap_net_raw+ep ${JAVA_HOME}/bin/java && \
    echo ${JAVA_HOME}/lib/jli > /etc/ld.so.conf.d/java-latest.conf && \
    ldconfig

VOLUME [ "/usr/share/sentinel/deploy", "/usr/share/sentinel/etc", "/usr/share/sentinel/data" ]

WORKDIR /usr/share/sentinel

### Containers should NOT run as root as a good practice
USER 10001

ENTRYPOINT [ "/entrypoint.sh" ]

STOPSIGNAL SIGTERM

CMD [ "-f" ]

# Arguments for labels should not invalidate caches
ARG VERSION
ARG SOURCE
ARG BUILD_JOB_ID
ARG BUILD_NUMBER
ARG BUILD_URL
ARG BUILD_BRANCH

LABEL org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.title="OpenNMS Sentinel ${VERSION}" \
      org.opencontainers.image.source="${SOURCE}" \
      org.opencontainers.image.revision="${REVISION}" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.vendor="The OpenNMS Group, Inc." \
      org.opencontainers.image.authors="OpenNMS Community" \
      org.opencontainers.image.licenses="AGPL-3.0" \
      org.opennms.image.base="${BASE_IMAGE}" \
      org.opennme.cicd.jobid="${BUILD_JOB_ID}" \
      org.opennms.cicd.buildnumber="${BUILD_NUMBER}" \
      org.opennms.cicd.buildurl="${BUILD_URL}" \
      org.opennms.cicd.branch="${BUILD_BRANCH}"

### Runtime information and not relevant at build time

# TODO MVR SENTINEL_LOCATION is not used at the moment
ENV SENTINEL_HOME="/usr/share/sentinel" \
    SENTINEL_ID="" \
    SENTINEL_LOCATION="SENTINEL" \
    OPENNMS_BROKER_URL="tcp://127.0.0.1:61616" \
    OPENNMS_HTTP_USER="minion" \
    OPENNMS_HTTP_PASS="minion" \
    OPENNMS_BROKER_USER="minion" \
    OPENNMS_BROKER_PASS="minion" \
    POSTGRES_HOST="localhost" \
    POSTGRES_PORT="5432" \
    POSTGRES_USER="postgres" \
    POSTGRES_PASSWORD="" \
    POSTGRES_DB="opennms"

##------------------------------------------------------------------------------
## EXPOSED PORTS
##------------------------------------------------------------------------------
## -- Sentinel Karaf Debug 5005/TCP
## -- Sentinel KARAF SSH   8301/TCP

EXPOSE 8301
