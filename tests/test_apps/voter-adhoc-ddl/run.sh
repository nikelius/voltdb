#!/usr/bin/env bash

declare ADHOC_METHOD=sqlcmd
declare PREPOPULATE_DATA=yes
declare PAUSE=no

declare PID_FILE=~/.voltdb_server/localhost_3021.pid
declare OUTPUT_FILE=~/.voltdb_server/localhost_3021.out
declare ERROR_FILE=~/.voltdb_server/localhost_3021.err
declare QUERY_OUTPUT_FILE=sql_last.out
declare SQL_OUTPUT_FILE=sql.out

if [ -n "$(which voltdb 2> /dev/null)" ]; then
    VOLTDB_BASE=$(dirname $(dirname $(which voltdb)))
else
    echo "The VoltDB scripts are not in your PATH."
    exit 1
fi

function _push() {
    pushd scenarios/$1 > /dev/null || exit 1
}

function _pop() {
    popd > /dev/null || exit 1
}

function _compile() {
    echo "Compiling Java..."
    mkdir -p obj
    local CP=$CLASSPATH:$({ \
        \ls -1 $VOLTDB_BASE/voltdb/voltdb-*.jar; \
        \ls -1 $VOLTDB_BASE/lib/*.jar; \
        \ls -1 $VOLTDB_BASE/lib/extension/*.jar; \
    } 2> /dev/null | paste -sd ':' - )
    javac -target 1.7 -source 1.7 -classpath $CP -d obj src/voter/*.java src/voter/procedures/*.java
    if [ $? -ne 0 ]; then
        echo "Java compilation failed."
        exit 1
    fi
}

function _catalog() {
    echo "Compiling catalog..."
    voltdb compile --classpath obj -o voter.jar ../../ddl.sql > catalog.out
    if [ $? -ne 0 ]; then
        cat catalog.out
        exit 1
    fi
}

function _atexit() {
    if [ $? -eq 0 ]; then
        echo SUCCESS
    else
        echo FAILURE
    fi
    voltdb stop
}

function _scenario() {
    echo "Context[$1]: BEGIN"
    pushd scenarios/$1 > /dev/null || exit 1
    trap "_pop; echo \"Context[$1]: END\"; trap RETURN" RETURN
    shift
    "$@"
}

function _die() {
    echo "FAILED: $@"
    echo "
>>> Output Log <<<
"
    test -e $OUTPUT_FILE && cat $OUTPUT_FILE
    echo "
>>> Error Log <<<
"
    test -e $ERROR_FILE && cat $ERROR_FILE
    exit 1
}

function _server() {
    echo "Starting server..."
    test -f $PID_FILE && rm -v $PID_FILE
    test -f $OUTPUT_FILE && rm -v $OUTPUT_FILE
    test -f $ERROR_FILE && rm -v $ERROR_FILE
    voltdb create -B -d ../../deployment.xml -l $VOLTDB_BASE/voltdb/license.xml voter.jar || exit 1
    trap "_atexit" EXIT
    local I=0
    while [ ! -e $PID_FILE ]; do
        test $I -eq 3 && _die "Gave up on server process creation."
        echo "Waiting for server process to get created..."
        sleep 5
        let I=I+1
    done
    local SERVER_PID=$(cat $PID_FILE)
    let I=0
    while ! grep -q 'Server completed initialization.' ~/.voltdb_server/localhost_3021.out; do
        test $I -eq 5 && _die "Gave up on server initialization."
        if ! kill -0 $SERVER_PID; then
            _die "Server process (PID=$SERVER_PID) died."
        fi
        echo "Waiting for server (PID=$SERVER_PID) to initialize..."
        sleep 5
        let I=I+1
    done
}

function _client() {
    echo "Running client..."
    local CP=$CLASSPATH:$({ \
        \ls -1 $VOLTDB_BASE/voltdb/voltdbclient-*.jar; \
        \ls -1 $VOLTDB_BASE/lib/commons-cli-1.2.jar; \
    } 2> /dev/null | paste -sd ':' - )
    java -classpath obj:$CP:obj -Dlog4j.configuration=file://$VOLTDB_BASE/voltdb/log4j.xml voter.TestClient || exit 1
}

function _ddl() {
    _sql_or_ddl DDL yes "$@"
}

function _ddl_bad() {
    _sql_or_ddl DDL no "$@"
}

function _sql() {
    _sql_or_ddl SQL yes "$@"
}

function _sql_or_ddl() {
    local TYPE=$1
    shift
    local SUCCEEDS=$1
    shift
    echo ">>> $TYPE: $@"
    local OKAY=no
    if [ "$ADHOC_METHOD" = "proccall" ]; then
        echo "Executing $TYPE via exec @AdHoc..."
        echo "exec @AdHoc '$@'" | sqlcmd --enable-adhoc-proc > $QUERY_OUTPUT_FILE && OKAY=yes
    else
        echo "Executing $TYPE by piping to sqlcmd..."
        echo "$@" | sqlcmd > $QUERY_OUTPUT_FILE  && OKAY=yes
    fi
    test "$TYPE" = "SQL" && cat $QUERY_OUTPUT_FILE
    cat $QUERY_OUTPUT_FILE >> $SQL_OUTPUT_FILE
    if [ "$SUCCEEDS" = "yes" -a "$OKAY" != "yes" ]; then
        echo "
>>> SQL/DDL Output <<<
"
        cat $SQL_OUTPUT_FILE
        _die "$TYPE query failed using $ADHOC_METHOD"
    elif [ "$SUCCEEDS" != "yes" -a "$OKAY" = "yes" ]; then
        echo "
>>> SQL/DDL Output <<<
"
        cat $SQL_OUTPUT_FILE
        _die "Expected bad $TYPE succeeded using $ADHOC_METHOD"
    fi
}

function _jar() {
    echo "Creating jar $1 from $2.class..."
    pushd obj > /dev/null || exit 1
    jar cf ../$1 voter/procedures/$2.class || exit 1
    popd > /dev/null || exit 1
}

function _start() {
    _compile
    _catalog
    _server
}

function _check_function() {
    if ! declare -Ff "$1" >/dev/null; then
        test -n "$3" && echo "* $2 $3 not found *"
        _usage
    fi
}

function _list_tests {
    set | awk '/_TEST_.*/{if ($2=="()") print substr($1, 7)}' | sort
}

function _list_commands {
    set | awk '/_RUN_.*/{if ($2=="()") print substr($1, 6)}' | sort
}

### Tests ###

function _TEST_base() {
    _scenario base _client
}

# SUCCESS: empty or non-empty
function _TEST_add-column() {
    _ddl "alter table votes add column ssn varchar(20);"
    _scenario base _client
}

# FAILURE: empty or non-empty
# Expected error: "Column SSN has no default and is not nullable."
function _TEST_add-not-null() {
    _ddl_bad "alter table votes add column ssn varchar(20) not null;"
    _scenario base _client
}

# SUCCESS: empty or non-empty
function _TEST_add-with-default() {
    _ddl "alter table votes add column ssn varchar(20) default 'SSN' not null;"
    _scenario base _client
}

# SUCCESS: empty or non-empty
function _TEST_column-and-proc() {
    _scenario column-and-proc _compile
    _scenario column-and-proc _jar update.jar Vote
    _scenario column-and-proc _ddl "\
alter table votes add column ssn varchar(20); \
exec @UpdateClasses 'update.jar' '';"
    _scenario column-and-proc _client
}

# SUCCESS: empty or non-empty
function _TEST_constraint() {
    _ddl "\
alter table votes add column ssn varchar(20) default 'SSN' not null; \
alter table votes add constraint CON_LIMIT_SSN limit partition rows 100;"
}

function _TEST_default() {
    _ddl "alter table votes add column ssn varchar(20);"
    _ddl "alter table votes alter ssn set default 'XXX-XX-XXXX';"
    _sql "insert into votes (phone_number, state, contestant_number) values ('1111111','WA',1111111)"
    if ! (_sql "select ssn from votes where phone_number = 1111111" | grep -q XXX-XX-XXXX); then
        _sql "select ssn from votes where phone_number = 1111111"
        _die "Failed to find expected SSN=XXX-XX-XXXX"
    fi
}

# SUCCESS: empty or non-empty
function _TEST_drop-column() {
    _ddl "alter table votes add column ssn varchar(20);"
    _sql "select * from votes limit 0" | grep PHONE_NUMBER
    _scenario base _client
    _ddl "alter table votes drop column ssn;"
    _sql "select * from votes limit 0" | grep PHONE_NUMBER
    _scenario base _client
}

### Top-level commands ###

function _RUN_clean() {
    if [ "$1" = "_USAGE_" ]; then
        echo "\
  Command: $2 clean

    Clean output files."
        return
    fi
    local D
    for D in . scenarios/*; do
        rm -rf $D/obj $D/debugoutput $D/*.jar $D/voltdbroot $D/log $D/*.txt \
               $D/statement-plans $D/catalog.out $D/catalog-report.html
    done
}

# argument 1: test name
function _RUN_test() {
    if [ "$1" = "_USAGE_" ]; then
        echo "\
  Command: $2 [OPTIONS] test NAME

    Run a test.

    OPTIONS
      -e use empty database - don't pre-populate
      -p pause between stages, e.g. for external queries
      -s use @AdHoc system procedure

    NAME"
        for T in $(_list_tests); do
            echo "      $T"
        done
        return
    fi
    if [ -z "$1" ]; then
        echo "* No test specified *"
        _usage
    fi
    local FUNCTION=_TEST_$1
    _check_function $FUNCTION test $1
    shift
    _RUN_clean
    _scenario base _start
    if [ "$PAUSE" = "yes" ]; then
        read -p "(pause)" LINE
    fi
    if [ "$PREPOPULATE_DATA" = "yes" ]; then
        _scenario base _client
        shift
    fi
    $FUNCTION "$@"
}

function _RUN_all() {
    if [ "$1" = "_USAGE_" ]; then
        echo "\
  Command: $2 all

    Run all tests."
        return
    fi
    for TEST in $(_list_tests); do
        if [ "$TEST" != "all" ]; then
            echo "
>>> TEST: $TEST <<<
"
            _RUN_test $TEST
            voltdb stop
        fi
    done
}

### Usage ###

function _usage() {
    echo "
Usage:
"
    local NAME=$(basename $0)
    for CMD in $(_list_commands); do
        "_RUN_$CMD" _USAGE_ $NAME
        echo ""
    done
    exit 1
}

### Main ###

args=$(getopt "eps" $*)
if [ $? != 0 ]; then
    _usage
fi
set -- $args
for ARG; do
   case "$ARG"
   in
   -e)
       export PREPOPULATE_DATA=no
       shift;;
   -p)
       export PAUSE=yes
       shift;;
   -s)
       export ADHOC_METHOD=proccall
       shift;;
   --)
       shift; break;;
   esac
done

declare FUNCTION="_RUN_$1"
_check_function $FUNCTION sub-command $1
shift

$FUNCTION "$@"
