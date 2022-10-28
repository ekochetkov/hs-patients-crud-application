#/bin/bash

# Open tunnel to db
while
  PG_LOCAL_PORT=$(shuf -n 1 -i 49152-65535)
  netstat -atun | grep -q "$PG_LOCAL_PORT"
do
  continue
done

echo "Use $PG_LOCAL_PORT port for connect to postgres in cluster"

kubectl --namespace=patients-crud-app port-forward svc/app-dbmaster $PG_LOCAL_PORT:5432 &

export DATABASE_URL_IN_CLUSTER=$(kubectl --namespace patients-crud-app get secret app-db-secret -o jsonpath='{.data.DATABASE_URL}' | base64 -d)

export DATABASE_URL="${DATABASE_URL_IN_CLUSTER//app-dbmaster:5432/localhost:$PG_LOCAL_PORT}"

# Make migrations
echo "Start seed data x500"
clojure -X:seed
echo "100"
clojure -X:seed
echo "200"
clojure -X:seed
echo "300"
clojure -X:seed
echo "400"
clojure -X:seed

# Close tunnel
kill $(jobs -p)

# Clean up
export -n PG_LOCAL_PORT DATABASE_URL_IN_CLUSTER DATABASE_URL
