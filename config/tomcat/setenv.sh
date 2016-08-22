export JAVA_HOME=/usr/java/default
export PATH=$JAVA_HOME/bin:$PATH
export CATALINA_HOME=/usr/share/tomcat7

# jvm options
JAVA_OPTS="$JAVA_OPTS -server"
JAVA_OPTS="$JAVA_OPTS -XX:NewSize=2560m -XX:MaxNewSize=5g -XX:InitialHeapSize=10g -XX:MaxHeapSize=10g"
JAVA_OPTS="$JAVA_OPTS -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m"
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:+DisableExplicitGC -XX:SurvivorRatio=15"
JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
JAVA_OPTS="$JAVA_OPTS -XX:+PrintTenuringDistribution -XX:+PrintGCTaskTimeStamps"
JAVA_OPTS="$JAVA_OPTS -Xloggc:/usr/share/tomcat7/logs/fcrepo-gc.log"
JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=/usr/share/tomcat7/logs/fedora_heap.hprof"

# Timezone and JVM file encoding
JAVA_OPTS="$JAVA_OPTS -Duser.timezone=UTC -Dfile.encoding=UTF8 -Djava.awt.headless=true"
export CATALINA_OPTS="-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.authenticate=true \
-Dcom.sun.management.jmxremote.password.file=/usr/share/tomcat7/conf/jmxremote.password \
-Dcom.sun.management.jmxremote.access.file=/usr/share/tomcat7/conf/jmxremote.access"
JAVA_OPTS="$JAVA_OPTS $CATALINA_OPTS -XX:OnOutOfMemoryError='kill -9 %p; /sbin/service tomcat7 start'"

# fedora 4 properties
FCREPO_HOME=/fedora_data
JAVA_OPTS="$JAVA_OPTS -Dfcrepo.home=$FCREPO_HOME"
JAVA_OPTS="$JAVA_OPTS -Dfcrepo.modeshape.index.directory=$FCREPO_HOME/fcrepo.index.directory"
JAVA_OPTS="$JAVA_OPTS -Dfcrepo.log.directory=/usr/share/tomcat7/logs"
JAVA_OPTS="$JAVA_OPTS -Dfcrepo.log.jcr=DEBUG"
JAVA_OPTS="$JAVA_OPTS -Dfcrepo.log.oai=TRACE"
JAVA_OPTS="$JAVA_OPTS -Dfcrepo.log.maxHistory=30"
JAVA_OPTS="$JAVA_OPTS -Dfcrepo.log.totalSizeCap=3G"

# Saxon tranformer factory - XSLT 2.0
JAVA_OPTS="$JAVA_OPTS -Djava.xml.transform.TransformerFactory=net.sf.saxon.TransformerFactoryImpl"

export JAVA_OPTS
export CATALINA_PID=$CATALINA_HOME/logs/tomcat.pid
