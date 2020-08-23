#!/bin/sh

home=$(cd `dirname $0`; cd ..; pwd)

. ${home}/bin/common.sh

${flume_home}/bin/flume-ng agent \
--conf ${flume_home}/conf \
-f ${conf_home}/collect.conf -n a1 \
-Dflume.monitoring.type=http \
-Dflume.monitoring.port=5653 \
>> ${logs_home}/collect.log 2>&1 &

echo $! > ${logs_home}/collect.pid
