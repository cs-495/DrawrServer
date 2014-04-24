#!/bin/bash

# benji@benji-PC /cygdrive/c/home/prog/workspace/DrawrServer
# $ java -cp bin com.meow.kittypaintdev.server.DrawrServer

cd /home/kat/DrawrServer
DRAWRPIDFILE=/var/run/drawrserver.pid

kill_daemon(){
    if [ -e "$DRAWRPIDFILE" ]
      then
        pid=`cat $DRAWRPIDFILE`
        if ps -p `cat $DRAWRPIDFILE` -o user,pid,args | grep DrawrServer #| grep java >/dev/null #2>&1
          then
            kill `cat $DRAWRPIDFILE`
			rm "$DRAWRPIDFILE"
			echo "Server was murdered."
		  else
		    echo "Server was not running."
        fi
	  else
	    echo "PIDFILE not found. Server probably dead."
    fi
}

#kill_daemon_actual(){
#    kill `cat $DRAWRPIDFILE`
#}

start_daemon(){
    #java -Ddaemon.pidfile=$DRAWRPIDFILE -cp bin com.meow.kittypaintdev.server.DrawrServer <&- 1>/dev/null 2>&1 &
    java -Ddaemon.pidfile=$DRAWRPIDFILE -cp bin com.meow.kittypaintdev.server.DrawrServer <&- 1>test.log 2>&1 &
    pid=$!
    echo ${pid} > $DRAWRPIDFILE
    echo "starting... pid = $pid"
}

if [ "$1" == "start" ]
  then
    kill_daemon
    start_daemon
fi

if [ "$1" == "stop" ]
  then
    kill_daemon
fi

if [ "$1" == "restart" ]
  then
    kill_daemon
    start_daemon
fi
