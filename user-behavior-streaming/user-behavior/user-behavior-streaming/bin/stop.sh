#!/bin/sh

home=$(cd `dirname $0`; cd ..; pwd)

echo "put 'user_behavior_status','status','status:status','stop'" | hbase shell

