#Dockerfile for running scalatra on jetty.
 
FROM bitnami/minideb:bookworm as build
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:11 $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

#Ant installation sequence mostly from https://github.com/frekele/docker-ant/blob/master/Dockerfile
ENV ANT_VERSION=1.10.12
ENV ANT_HOME=/opt/ant

WORKDIR /tmp
RUN apt-get update && apt-get install -y unzip wget

# Download and extract apache ant to opt folder
RUN wget --no-check-certificate --no-cookies http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz \
    && wget --no-check-certificate --no-cookies http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz.sha512 \
    && echo "$(cat apache-ant-${ANT_VERSION}-bin.tar.gz.sha512) apache-ant-${ANT_VERSION}-bin.tar.gz" | sha512sum -c \
    && tar -zvxf apache-ant-${ANT_VERSION}-bin.tar.gz -C /opt/ \
    && ln -s /opt/apache-ant-${ANT_VERSION} /opt/ant \
    && rm -f apache-ant-${ANT_VERSION}-bin.tar.gz \
    && rm -f apache-ant-${ANT_VERSION}-bin.tar.gz.sha512

# add executables to path
RUN update-alternatives --install "/usr/bin/ant" "ant" "/opt/ant/bin/ant" 1 && \
    update-alternatives --set "ant" "/opt/ant/bin/ant" 

#Panomicon build process from here
ENV SCALA_VERSION=2.12.17

RUN wget https://downloads.lightbend.com/scala/${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz \
  && tar xzf scala-${SCALA_VERSION}.tgz -C /opt/ \
  && rm -f scala-${SCALA_VERSION}.tgz

ENV SCALA_HOME=/opt/scala-${SCALA_VERSION}

COPY ivy.xml ivy_cache.xml ivy.jar /build/
#Populate the ivy cache (slow step, helpful to do early so we can cache it)
RUN ant -lib /build -f /build/ivy_cache.xml

COPY OTGTool /build/OTGTool/
COPY Toxygates /build/Toxygates/
COPY build_common.xml /build/
WORKDIR /build/Toxygates

#Download GWT
ENV GWT_VERSION=2.9.0
RUN wget https://storage.googleapis.com/gwt-releases/gwt-${GWT_VERSION}.zip \
  && unzip gwt-${GWT_VERSION}.zip -d /opt/ \
  && rm -f gwt-${GWT_VERSION}.zip
ENV GWT_SDK=/opt/gwt-${GWT_VERSION}  

RUN ant -lib /build scalatra-jetty

#Build done, set up the final runtime image
FROM bitnami/minideb:bookworm as main
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:11 $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY --from=build /build/Toxygates/scalatra-jetty /panomicon
COPY docker/scalatra/panomicon/* /panomicon/

ENTRYPOINT [ "/panomicon/panomicon.sh" ]
CMD []
