#!/bin/bash
# http://www.db.informatik.uni-bremen.de/publications/intern/4bach/nisha-batch/
# https://www.gnu.org/software/coreutils/manual/html_node/timeout-invocation.html

# Konfiguration der Umgebung und maximalen Ausführungszeit
filepath=/root/masterarbeit
usepath=/root/use-4.2.0-481/bin/use
timeoutduration=90m

# Konfiguration der Logdateien (werden im Ausführungsverzeichnis erstellt)
logtestnc04=log_test_nc04.txt
logtestnc06=log_test_nc06.txt
logtestnc08=log_test_nc08.txt
logtestnc10=log_test_nc10.txt
logtestnc12=log_test_nc12.txt
logtestnc14=log_test_nc14.txt

echo "Start: $(date)"

echo "===== TESTCASE NC04 START =====" > $logtestnc04
echo "===== TESTCASE NC04 SAT4J =====" >> $logtestnc04
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc04_sat4j.soil >> $logtestnc04
  killall java &> /dev/null
done
echo "===== TESTCASE NC04 MINISAT =====" >> $logtestnc04
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc04_minisat.soil >> $logtestnc04
  killall java &> /dev/null
done
echo "===== TESTCASE NC04 LINGELING =====" >> $logtestnc04
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc04_lingeling.soil >> $logtestnc04
  killall java &> /dev/null
done
echo "===== TESTCASE NC04 PLINGELING =====" >> $logtestnc04
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc04_plingeling.soil >> $logtestnc04
  killall java &> /dev/null
  killall plingeling &> /dev/null
done
echo "===== TESTCASE NC04 END =====" >> $logtestnc04

echo "===== TESTCASE NC06 START =====" > $logtestnc06
echo "===== TESTCASE NC06 SAT4J =====" >> $logtestnc06
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc06_sat4j.soil >> $logtestnc06
  killall java &> /dev/null
done
echo "===== TESTCASE NC06 MINISAT =====" >> $logtestnc06
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc06_minisat.soil >> $logtestnc06
  killall java &> /dev/null
done
echo "===== TESTCASE NC06 LINGELING =====" >> $logtestnc06
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc06_lingeling.soil >> $logtestnc06
  killall java &> /dev/null
done
echo "===== TESTCASE NC06 PLINGELING =====" >> $logtestnc06
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc06_plingeling.soil >> $logtestnc06
  killall java &> /dev/null
  killall plingeling &> /dev/null
done
echo "===== TESTCASE NC06 END =====" >> $logtestnc06

echo "===== TESTCASE NC08 START =====" > $logtestnc08
echo "===== TESTCASE NC08 SAT4J =====" >> $logtestnc08
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc08_sat4j.soil >> $logtestnc08
  killall java &> /dev/null
done
echo "===== TESTCASE NC08 MINISAT =====" >> $logtestnc08
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc08_minisat.soil >> $logtestnc08
  killall java &> /dev/null
done
echo "===== TESTCASE NC08 LINGELING =====" >> $logtestnc08
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc08_lingeling.soil >> $logtestnc08
  killall java &> /dev/null
done
echo "===== TESTCASE NC08 PLINGELING =====" >> $logtestnc08
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc08_plingeling.soil >> $logtestnc08
  killall java &> /dev/null
  killall plingeling &> /dev/null
done
echo "===== TESTCASE NC08 END =====" >> $logtestnc08

echo "===== TESTCASE NC10 START =====" > $logtestnc10
echo "===== TESTCASE NC10 SAT4J =====" >> $logtestnc10
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc10_sat4j.soil >> $logtestnc10
  killall java &> /dev/null
done
echo "===== TESTCASE NC10 MINISAT =====" >> $logtestnc10
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc10_minisat.soil >> $logtestnc10
  killall java &> /dev/null
done
echo "===== TESTCASE NC10 LINGELING =====" >> $logtestnc10
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc10_lingeling.soil >> $logtestnc10
  killall java &> /dev/null
done
echo "===== TESTCASE NC10 PLINGELING =====" >> $logtestnc10
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc10_plingeling.soil >> $logtestnc10
  killall java &> /dev/null
  killall plingeling &> /dev/null
done
echo "===== TESTCASE NC10 END =====" >> $logtestnc10

echo "===== TESTCASE NC12 START =====" > $logtestnc12
echo "===== TESTCASE NC12 SAT4J =====" >> $logtestnc12
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc12_sat4j.soil >> $logtestnc12
  killall java &> /dev/null
done
echo "===== TESTCASE NC12 MINISAT =====" >> $logtestnc12
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc12_minisat.soil >> $logtestnc12
  killall java &> /dev/null
done
echo "===== TESTCASE NC12 LINGELING =====" >> $logtestnc12
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc12_lingeling.soil >> $logtestnc12
  killall java &> /dev/null
done
echo "===== TESTCASE NC12 PLINGELING =====" >> $logtestnc12
for i in {1..3}; do
  timeout -s 15 $timeoutduration $usepath -nogui ${filepath}/Topologiemodell/networktopology.use ${filepath}/Performancetest/test_nc12_plingeling.soil >> $logtestnc12
  killall java &> /dev/null
  killall plingeling &> /dev/null
done
echo "===== TESTCASE NC12 END =====" >> $logtestnc12

echo "End: $(date)"
