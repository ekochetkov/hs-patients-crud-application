#/bin/bash

IMAGE=$1

CURRENT_IMAGE=$(kubectl --namespace patients-crud-app get pods --selector project=app -o jsonpath='{.items[0].spec.containers[0].image}')

echo "$(date) | Current image: $CURRENT_IMAGE"
echo "$(date) | Update image to: $IMAGE"

docker manifest inspect $IMAGE > /dev/null || { echo "$(date) | $IMAGE not exists!" ; exit 1; }

echo "$(date) | Image exists in Docker hub"

echo -n "$(date) | Run test pod: "

TEST_POD_NAME="some-pod-name-for-run-in-cluster-only"
TEST_POD_PORT=40001

kubectl --namespace patients-crud-app \
	run $TEST_POD_NAME \
	--image $IMAGE \
	--env PORT=$TEST_POD_PORT \
	--env WS_URL="not used in this case" \
	--env DATABASE_URL=$(kubectl --namespace patients-crud-app get secret app-db-secret -o jsonpath='{.data.DATABASE_URL}' | base64 -d) || { echo "Error with create test pod. Exit." ; exit 1; }

while
    phase=$(kubectl --namespace patients-crud-app get pod $TEST_POD_NAME -o jsonpath='{.status.phase}')
    echo "$(date) | Current pod phase: $phase"    
    [ ! $phase == "Running" ]
do
  sleep 1s      
done

echo "$(date) | Pod successful run"

while
    POD_LOCAL_PORT=$(shuf -n 1 -i 49152-65535)
    netstat -atun | grep -q "$POD_LOCAL_PORT"
do
    continue
done

TUNNEL_PID='no-pid'
FORWARD_LOG=$(mktemp)

while
    true
do
    if [ ! -d "/proc/$TUNNEL_PID" ];
    then
	if [ -s $FORWARD_LOG ];
	then
          echo "$(date) | Lost connection to pod. Try again..."
        fi	
	kubectl --namespace=patients-crud-app \
		port-forward \
		pod/$TEST_POD_NAME \
		$POD_LOCAL_PORT:$TEST_POD_PORT 2> $FORWARD_LOG > /dev/null & TUNNEL_PID=$!
	echo "$(date) | Port forward process pid: $TUNNEL_PID"
    fi    
    health=$(curl -s localhost:$POD_LOCAL_PORT/health || echo "wait")
    echo "$(date) | App health: $health"    
    if [ ! $health == "wait" ];
    then
       break
    fi	
    sleep 1s
done

rm $FORWARD_LOG
kill $TUNNEL_PID

echo -n "$(date) | Delete test pod: "
kubectl --namespace patients-crud-app \
	delete pod $TEST_POD_NAME || { echo "Error with delete test pod. Exit." ; exit 1; }

if [ $health == "ok" ]
then
    echo "$(date) | Ready for upgrade helm app deploy"
    while true; do
      read -p "Do you wish to set $IMAGE? Check db migrations before. " yn
      case $yn in
          [Yy]* )
	      echo "$(date) | let's go";

	      helm upgrade app k8s/chart \
		   --reuse-values=true \
		   --namespace patients-crud-app \
		   --set main.image=$IMAGE

	      echo "$(date) | End. Exit"
	      
	      break;;
        [Nn]* ) echo "$(date) | Aborting. Exit"; break;;
        * ) echo "Please answer yes or no.";;
      esac
    done

    
else
    echo "$(date) | App health fail. Logs:"
    kubectl --namespace patients-crud-app logs $TEST_POD_NAME
    echo "$(date) | End logs."    
fi
