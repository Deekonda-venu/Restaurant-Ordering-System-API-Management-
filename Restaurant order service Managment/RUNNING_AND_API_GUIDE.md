# Restaurant Ordering System: Run and API Guide

This guide describes the current local setup, the APIs exposed by each service, and how a request moves through the system. The detailed request and response examples for the synchronous APIs are in [API_Documentation.md](API_Documentation.md).

## 1. Services and ports

| Service | Port | API routes | Storage |
|---|---:|---|---|
| Resturant | 9191 | `/API/resturant/v1` | MySQL `resturant` |
| MenuItemsService | 9292 | `/API/menuitems/v1` | MySQL `MenuItems` |
| Customer-Service | 9293 | `/API/customer/v1` | MySQL `CustomerDetails` |
| Payment-Service | 9295 | `/API/Payments/v1` | MySQL `PaymentService` |
| Order_Service | 9999 | `/API/Order/v1` | MySQL `OrderService` |
| Kitchen-Serivce | 9296 | `/api/kitchen` | MongoDB `kitchen_db` |
| Notification-Serivce | 9297 | No HTTP routes; consumes Kafka events | MongoDB `notification_db` configured, no notification repository currently used |
| Delivary-Serivce | 9298 | `/api/deliveries` | MongoDB `delivery_db` |
| Kafka | 9092 | Event broker | Docker container |
| Kafka UI | 8090 | Browser UI | Docker container |
| MongoDB | 27017 | Document database | Docker container |
| Mongo Express | 8091 | Browser UI | Docker container |
| MySQL | 3306 | Relational database | Local install or optional Docker container |

> Order runs on **9999**. Payment's Feign client also calls `http://localhost:9999`. Do not use old `9294` instructions for current requests. The port-conflict guide mentions 9294 only as the previous configuration.

## 2. Prerequisites

- Java 17
- Docker Desktop running, with Docker Compose available
- Either a MySQL server on `localhost:3306` with `root` / `root`, or the MySQL Docker profile below
- PowerShell on Windows (commands below assume you start at the repository root)

## 3. Start infrastructure

Open PowerShell in the repository root, then move into the service directory:

```powershell
Set-Location ".\Restaurant order service Managment"
```

If you have an existing local MySQL server using port 3306, start the non-MySQL infrastructure and use that MySQL instance:

```powershell
docker compose -f docker-compose-infra.yml up -d
Get-Content .\mysql\init.sql | mysql -uroot -proot
```

If you want Docker to run MySQL too, make sure no other MySQL server is using port 3306, then run:

```powershell
docker compose --profile mysql -f docker-compose-infra.yml up -d
```

The MySQL init script creates `CustomerDetails`, `resturant`, `MenuItems`, `OrderService`, and `PaymentService`. It runs only when Docker initializes a new MySQL data volume. To initialize schemas on an existing volume, run:

```powershell
Get-Content .\mysql\init.sql | docker compose --profile mysql -f docker-compose-infra.yml exec -T mysql mysql -uroot -proot
```

Check containers and logs:

```powershell
docker compose -f docker-compose-infra.yml ps
docker compose -f docker-compose-infra.yml logs -f kafka mongodb
```

Useful browser pages: Kafka UI at `http://localhost:8090`; Mongo Express at `http://localhost:8091`.

## 4. Start the microservices

Run each command in a separate PowerShell terminal and leave it running. From the repository root, start the synchronous services first because Order calls Customer, Restaurant, and Menu over HTTP, and Payment calls Order and Customer.

```powershell
Set-Location ".\Restaurant order service Managment\Resturant"; .\mvnw.cmd spring-boot:run
```

```powershell
Set-Location ".\Restaurant order service Managment\MenuItemsService"; .\mvnw.cmd spring-boot:run
```

```powershell
Set-Location ".\Restaurant order service Managment\Customer-Service"; .\mvnw.cmd spring-boot:run
```

```powershell
Set-Location ".\Restaurant order service Managment\Order_Service"; .\mvnw.cmd spring-boot:run
```

```powershell
Set-Location ".\Restaurant order service Managment\Payment-Service"; .\mvnw.cmd spring-boot:run
```

Then start the Kafka consumers and delivery workflow services:

```powershell
Set-Location ".\Restaurant order service Managment\Kitchen-Serivce"; .\gradlew.bat bootRun
```

```powershell
Set-Location ".\Restaurant order service Managment\Notification-Serivce"; .\gradlew.bat bootRun
```

```powershell
Set-Location ".\Restaurant order service Managment\Delivary-Serivce"; .\gradlew.bat bootRun
```

Wait for each application log to say `Started ...` and confirm that the HTTP services report their configured port. Kafka consumers should log their topic subscriptions. Do not start a second copy of a service on the same port.

## 5. API list

All paths below are appended to `http://localhost:<port>`. Use [API_Documentation.md](API_Documentation.md) for full request/response bodies on customer, restaurant, menu, order, and payment APIs.

| Service | Method and path | Purpose |
|---|---|---|
| Customer | `POST /API/customer/v1/CreateCustomer` | Create customer |
| Customer | `GET /API/customer/v1/GetAllCustomerDetails` | List customers |
| Customer | `GET /API/customer/v1/GetCustomerById/{id}` | Get customer |
| Customer | `GET /API/customer/v1/GetCustomerDetailsById/{customer_id}/Addresses` | Get customer and addresses |
| Customer | `GET /API/customer/v1/GetCustomerDetailsById/{customer_id}/Addresses/{address_id}` | Get one address |
| Customer | `POST /API/customer/v1/CreateAddressDetails/{Customerid}/Address` | Add an address |
| Customer | `PUT /API/customer/v1/UpdateFullCustomerDetails/{id}` | Replace customer details |
| Customer | `DELETE /API/customer/v1/DeleteCustomerDetails/{id}` | Delete customer |
| Customer | `DELETE /API/customer/v1/DeleteAddressByCustomerId/{customer_id}/addresses/{address_id}` | Delete address |
| Restaurant | `POST /API/resturant/v1/AddResturntdetails` | Create restaurant (defaults to OPEN) |
| Restaurant | `GET /API/resturant/v1/GetAllResturnentdetails` | List restaurants |
| Restaurant | `GET /API/resturant/v1/GetResturnentdetailsById/{id}` | Get restaurant and menu |
| Restaurant | `PUT /API/resturant/v1/UpdateResturnentDetailsByID/{id}` | Replace restaurant details |
| Restaurant | `PATCH /API/resturant/v1/UpdateResturnentDetailsByID/{id}` | Partially update restaurant |
| Restaurant | `DELETE /API/resturant/v1/DeleteResturentDetailsById/{id}` | Delete restaurant |
| Menu | `POST /API/menuitems/v1/saveMenuItems` | Create menu item |
| Menu | `GET /API/menuitems/v1/GetAllFooditems` | List menu items |
| Menu | `GET /API/menuitems/v1/GetFooditemsById/{ID}` | Get menu item and restaurant |
| Menu | `GET /API/menuitems/v1/GetMenuItemsByResturant/{resturantId}` | List restaurant's menu |
| Menu | `GET /API/menuitems/v1/GetByResturant/{resturantId}?category=Pizza&veg=true` | Filter menu |
| Menu | `PUT /API/menuitems/v1/updateMenuitemsById/{ID}` | Replace menu item |
| Menu | `PATCH /API/menuitems/v1/PatchMenuitemsById/{Id}` | Partially update menu item |
| Menu | `DELETE /API/menuitems/v1/deleteMenuitemsById/{ID}` | Delete menu item |
| Order | `POST /API/Order/v1/CreateOrder` | Validate referenced records, calculate totals, save and publish `ORDER_PLACED` |
| Order | `GET /API/Order/v1/GetOrderById/{OrderId}` | Get an enriched order |
| Order | `GET /API/Order/v1/GetAllOrders` | List orders |
| Order | `GET /API/Order/v1/GetAllOrdersByCustomerId/{CustomerId}` | List a customer's orders |
| Order | `GET /API/Order/v1/GetAllOrdersByResturantId/{ResturantId}` | List a restaurant's orders |
| Order | `PATCH /API/Order/v1/orders/{orderId}/status` | Update order status; send the status as a JSON string |
| Order | `POST /API/Order/v1/orders/{orderId}/cancel` | Cancel order |
| Payment | `POST /API/Payments/v1/payment` | Record payment and publish payment event |
| Payment | `GET /API/Payments/v1/payment/{id}` | Get payment by payment ID |
| Payment | `GET /API/Payments/v1/order/{orderId}` | List payments for an order |
| Payment | `POST /API/Payments/v1/{paymentId}/refund` | Refund a successful payment |
| Kitchen | `PATCH /api/kitchen/orders/{orderId}/status` | Set `PREPARING` or `READY` and publish kitchen event |
| Delivery | `POST /api/deliveries/{orderId}/assign-driver` | Assign driver; JSON body: `{"driverId": 12}` |
| Delivery | `PATCH /api/deliveries/{orderId}/status` | Set `PICKED_UP`, `ON_THE_WAY`, or `DELIVERED` |

Notification currently exposes **no REST API**. It listens to Kafka and prints the notification text in its console.

## 6. How the system works

1. A client creates a customer, adds a delivery address, creates an OPEN restaurant, and adds menu items.
2. The client calls Order `POST /API/Order/v1/CreateOrder`. Order synchronously checks customer, address, restaurant, and menu using Feign clients. It calculates the subtotal, 5% tax, flat delivery fee, and total, saves the order in MySQL, then publishes `ORDER_PLACED` to Kafka topic `order-events`.
3. Kitchen consumes `order-events` and stores a `RECEIVED` ticket in MongoDB. Notification consumes events and prints user-facing updates.
4. A kitchen operator changes the ticket to `PREPARING`, then `READY` through the Kitchen API. Kitchen publishes `ORDER_PREPARING` and `ORDER_READY` on `kitchen-events`.
5. Delivery consumes `ORDER_READY`, creates a `READY_FOR_PICKUP` record, and stores it in MongoDB. Assign a driver and update its delivery state through the Delivery API.
6. Payment records payment in MySQL and publishes `PAYMENT_SUCCESS` or `PAYMENT_FAILED`; Delivery publishes pickup and delivery events. Notification consumes the topics and prints corresponding messages.

| Kafka topic | Producer | Consumers |
|---|---|---|
| `order-events` | Order | Kitchen, Notification |
| `payment-events` | Payment | Notification |
| `kitchen-events` | Kitchen | Delivery, Notification |
| `delivery-events` | Delivery | Notification |

Order-to-Customer/Restaurant/Menu checks and Payment-to-Order checks are synchronous HTTP calls. Kitchen, Notification, and Delivery updates are asynchronous Kafka events, so their log output may appear shortly after the original request succeeds.

## 7. Smoke tests

After creating valid customer/address/restaurant/menu records, create an order using their IDs:

```powershell
$order = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:9999/API/Order/v1/CreateOrder" `
  -ContentType "application/json" `
  -Body '{"customerId":1,"restaurantId":1,"deliveryAddressId":1,"items":[{"menuItemId":1,"quantity":2}]}'
$orderId = $order.orderId
```

Use the returned `orderId` to advance the kitchen and delivery workflow:

```powershell
Invoke-RestMethod -Method Patch -Uri "http://localhost:9296/api/kitchen/orders/$orderId/status" -ContentType "application/json" -Body '{"status":"PREPARING"}'
Invoke-RestMethod -Method Patch -Uri "http://localhost:9296/api/kitchen/orders/$orderId/status" -ContentType "application/json" -Body '{"status":"READY"}'
Invoke-RestMethod -Method Post -Uri "http://localhost:9298/api/deliveries/$orderId/assign-driver" -ContentType "application/json" -Body '{"driverId":12}'
Invoke-RestMethod -Method Patch -Uri "http://localhost:9298/api/deliveries/$orderId/status" -ContentType "application/json" -Body '{"status":"PICKED_UP"}'
Invoke-RestMethod -Method Patch -Uri "http://localhost:9298/api/deliveries/$orderId/status" -ContentType "application/json" -Body '{"status":"ON_THE_WAY"}'
Invoke-RestMethod -Method Patch -Uri "http://localhost:9298/api/deliveries/$orderId/status" -ContentType "application/json" -Body '{"status":"DELIVERED"}'
```

Watch the Kitchen, Delivery, and Notification terminals for `KITCHEN:`, `DELIVERY:`, and `NOTIFICATION:` messages. Confirm MongoDB/Kafka are running before testing the asynchronous steps.

## 8. Common startup problems

- `Port ... was already in use`: keep one instance per service; find the process with `Get-NetTCPConnection -LocalPort <port>` and stop only the duplicate process.
- `Communications link failure`: verify MySQL is listening on 3306, the five schemas exist, and the username/password match each service's `application.properties`.
- Kafka connection refused on 9092: start Docker infrastructure and check `docker compose -f docker-compose-infra.yml ps`.
- Mongo connection refused on 27017: start the `mongodb` container.
- Kitchen/Delivery does not react: ensure its consumer is running, then inspect Kafka UI topics and the service console.

For port troubleshooting, see [PORT_CONFLICT_TROUBLESHOOTING.md](PORT_CONFLICT_TROUBLESHOOTING.md).
