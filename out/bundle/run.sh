DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
export MPJ_HOME=$DIR/mpj
export PATH=$MPJ_HOME/bin:$PATH
echo $MPJ_HOME
echo $DIR
mpjrun.sh -np $1 -jar '$DIR/checker.jar' $2 $3