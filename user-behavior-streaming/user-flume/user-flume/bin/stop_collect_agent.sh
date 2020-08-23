#!/bin/sh

home=$(cd `dirname $0`; cd ..; pwd)

. ${home}/bin/common.sh

pid=`cat ${logs_home}/collect.pid | head -1`

kill ${pid}
