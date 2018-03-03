#!/usr/bin/env bash
trap "exit" INT TERM # trap lines make it so taht when this script terminates the background java process does as well
trap "kill 0" EXIT

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

BAYOU_JAR="$(ls $SCRIPT_DIR/*.jar)"
cd $SCRIPT_DIR # log4j treats config paths relataive to current directory.  we need this so logs is next to the jar file and not the directory from where the script is being run

if [ $# -eq 0 ]
  then
    LOGS_DIR=$SCRIPT_DIR/logs
  else
    LOGS_DIR=$1
fi

mkdir -p "$LOGS_DIR"

export PYTHONPATH=$SCRIPT_DIR/python
python3 $SCRIPT_DIR/python/bayou/server/ast_server.py --save_dir "$SCRIPT_DIR/resources/model" --logs_dir "$LOGS_DIR"
