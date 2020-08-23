#!/bin/sh

home=$(cd `dirname $0`; cd ..; pwd)

. ${home}/bin/common.sh

echo "put 'user_behavior_status','status','status:status','run'" | hbase shell

${spark_submit} \
--master yarn \
--deploy-mode cluster \
--name user-behavior \
--queue root.realtime \
--driver-memory 6G \
--executor-memory 6G \
--executor-cores 4 \
--num-executors 2 \
--jars $(echo ${lib_home}/*.jar | tr ' ' ',') \
--class com.djt.streaming.UserBehaviorStreaming \
${lib_home}/user-behavior-1.0-SNAPSHOT.jar \
>> ${logs_home}/behavior.log 2>&1 &
