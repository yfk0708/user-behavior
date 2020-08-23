#!/bin/sh

home=$(cd `dirname $0`; cd ..; pwd)
bin_home=$home/bin
conf_home=$home/conf
logs_home=$home/logs
lib_home=$home/lib

spark_submit=/opt/modules/spark-2.3.0-bin-hadoop2.6/bin/spark-submit
