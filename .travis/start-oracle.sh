#!/usr/bin/env bash
# Oracle XE 18.0.0.0
# this docker image has the following users/credentials (user/password = system/oracle)
docker pull larmic/oracle-xe:18.4.0

# start the dockerized oracle-xe instance
# this container can be stopped using:
#
#    docker stop oracle
#
docker run --rm -p 1521:1521 --cpus=2 --name schemaspy -h schemaspy -d larmic/oracle-xe:18.4.0

printf "\n\nStarting Oracle XE container, this could take a few minutes..."
printf "\nWaiting for Oracle XE database to start up.... "
_WAIT=0;
while :
do
    printf " $_WAIT"
    if $(docker logs schemaspy | grep -q 'status READY'); then
        printf "\nOracle XE Database started\n\n"
        break
    fi
    sleep 10
    _WAIT=$(($_WAIT+10))
done

# docker ps -a
# print logs
docker logs schemaspy
