FROM thereisnospoon/ffmpeg-java
MAINTAINER thereisnospoon
COPY target/scala-2.12/upload-assembly-1.0.jar upload.jar
ENTRYPOINT ["java","-jar","upload.jar"]
