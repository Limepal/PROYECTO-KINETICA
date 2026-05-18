#!/usr/bin/env bash

# stiff<3

set -a
source ./.env
set +a

./mvnw spring-boot:run
