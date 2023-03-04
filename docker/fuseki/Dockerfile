FROM stain/jena-fuseki

#Override the entrypoint in the parent image, which doesn't exit cleanly when sent SIGTERM 
#(forces docker to send SIGKILL)
ENTRYPOINT [ "/jena-fuseki/fuseki-server" ]
CMD []
