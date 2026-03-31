@echo off
echo Starting Akka Cluster Nodes...

rem Node 1 (Seed Node)
start "Node 1 - Port 2551" java -cp "target/InventoryManagement-1.0-SNAPSHOT.jar" com.mani.InventoryApplication 2551 8081

rem Wait for seed node to begin starting
ping 127.0.0.1 -n 4 > NUL

rem Node 2
start "Node 2 - Port 2552" java -cp "target/InventoryManagement-1.0-SNAPSHOT.jar" com.mani.InventoryApplication 2552 8082

rem Wait a moment
ping 127.0.0.1 -n 2 > NUL

rem Node 3 
start "Node 3 - Port 2553" java -cp "target/InventoryManagement-1.0-SNAPSHOT.jar" com.mani.InventoryApplication 2553 8083

echo All nodes are starting in separate windows!
