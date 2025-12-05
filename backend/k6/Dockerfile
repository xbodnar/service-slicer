FROM grafana/k6:latest

# Directory inside the image where scripts will live
WORKDIR /scripts

# Copy all k6 scripts from this folder into the image
COPY experiment-template.js validation-template.js ./

# Default entrypoint is "k6" in the base image already,
# so this line is optional:
ENTRYPOINT ["k6"]
