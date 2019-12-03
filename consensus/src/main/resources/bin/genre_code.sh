dir=`pwd`
dir=${dir}/com/zstore/consensus/raft/proto
echo ${dir}/store.proto
protoc --java_out=. -I=${dir} ${dir}/*.proto