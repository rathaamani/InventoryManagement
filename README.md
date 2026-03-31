# Akka Cluster Inventory Management System

A high-performance, distributed inventory management system built with **Akka Typed (Java)** and **Vert.x Web**. This project demonstrates how to use Akka Cluster Sharding to manage state across multiple nodes while providing a REST API via Vert.x.

## 🚀 Features
- **Akka Cluster Sharding**: Product inventory state is distributed across the cluster. Each product name/ID is managed by a dedicated actor (Entity).
- **Vert.x HTTP API**: Lightweight, non-blocking REST endpoints to interact with the actors.
- **Failover & Scalability**: The cluster automatically manages where product actors live, ensuring high availability.
- **Maven Shade Plugin**: Perfectly configured to create an executable fat JAR with all dependencies aligned via `akka-bom`.

---

## 🛠️ Prerequisites
- **Java 11** or higher
- **Maven 3.6+**

---

## 📦 Building the Project
Run the following command to clean, compile, and package the project into a fat JAR:
```bash
mvn clean package
```
This generates `target/InventoryManagement-1.0-SNAPSHOT.jar`.

---

## 🚦 Running the Cluster Locally

### Quick Start (Windows)
Run the provided batch file to launch 3 nodes (Seed on 2551, Workers on 2552 & 2553):
```powershell
.\start-cluster.bat
```

### Manual Node Start
To start a node manually, use the following syntax:
`java -cp target/InventoryManagement-1.0-SNAPSHOT.jar com.mani.InventoryApplication <akka-port> <http-port>`

**Example:**
```bash
java -cp target/InventoryManagement-1.0-SNAPSHOT.jar com.mani.InventoryApplication 2551 8081
```

---

## 🌐 API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/health` | Check node health and system time. |
| `GET` | `/products` | List all catalog product IDs. |
| `GET` | `/inventory/:productId` | Check current stock/reservation for a product. |
| `POST` | `/orders` | Reserve stock for a specific product. |

### Example API Usage (PowerShell)

**Check Stock:**
```powershell
Invoke-RestMethod -Uri http://localhost:8081/inventory/LAPTOP-001
```

**Place Order:**
```powershell
Invoke-RestMethod -Uri http://localhost:8081/orders -Method Post -Body '{"productId": "LAPTOP-001", "quantity": 5}' -ContentType "application/json"
```

---

## 📂 Project Structure
- `com.mani.InventoryApplication`: Main entry point.
- `com.mani.akka.actors`: Akka Guardian and Product Actors.
- `com.mani.akka.messages`: Command and Response objects for Cluster Sharding.
- `com.mani.vertx`: Vert.x HTTP Verticle and API routing.
- `src/main/resources/inventory.conf`: Akka Cluster configuration.

---

## 📝 Troubleshooting
If you see `java.net.BindException`, a previous instance of the application might still be running. Use `Stop-Process -Name "java" -Force` in PowerShell to clear them before restarting the cluster.
