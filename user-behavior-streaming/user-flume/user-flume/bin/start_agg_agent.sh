#!/bin/sh

home=$(cd `dirname $0`; cd ..; pwd)

. ${home}/bin/common.sh

classpath=""

for file in ${lib_home}/*
do
    if [ -z ${classpath} ]; then
        classpath=${file}
    else
        classpath="${classpath}:${file}"
    fi
done

${flume_home}/bin/flume-ng agent \
--classpath ${classpath} \
--conf ${flume_home}/conf \
-f ${conf_home}/agg.conf -n a1 \
-Dflume.monitoring.type=http \
-Dflume.monitoring.port=5654 \
>> ${logs_home}/agg.log 2>&1 &

echo $! > ${logs_home}/agg.pid
