# regenerate graphs
/home/test/tools/vis.py ~/voltbin/mysqlp /home/test/.hudson/userContent/performance perf 800 320
python ~/trunk/tools/vis-micro-hudson.py ~/voltbin/mysqlp ~/.hudson/userContent/microbenchmark 30DaySpan 30
# clean up stray java processes
ps -ef | grep java | grep -v grep | grep -v hudson.war | sed 's/test *//g' | sed 's/ .*//g' | xargs -r kill -9
# check state of cluster machines
for i in 1 2 3a 3b 3c 3d 3e 3f 3g 3h 3i 3j 3k 3l 4a 4b 4c; do ssh volt$i tools/check-machine.sh; done
# check disk space on hzproject.com
ssh hudson@hzproject.com df . | tail -1 | awk "(\$5 > 75) {exit 1}"
# echo date of current build
ls -ltr ../builds | tail -1 | awk "{print \$8}"
