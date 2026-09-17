# Food Delivery Microservices — API Documentation

A synchronous (OpenFeign) microservices system. Each service owns its own database and communicates with others via Feign clients.

## Services & Ports

| Service | Base URL | Port | Database |
|---|---|---|---|
| Customer-Service | `/API/customer/v1` | `9293` | `CustomerService` |
| Resturant | `/API/resturant/v1` | `9191` | `ResturantService` |
| MenuItemsService | `/API/menuitems/v1` | `9292` | `MenuService` |
| Order_Service | `/API/Order/v1` | `9294` | `OrderService` |
| Payment-Service | `/API/Payments/v1` | `9295` | `PaymentService` |

---

# 1. Customer-Service (`http://localhost:9293`)

## POST `/API/customer/v1/CreateCustomer`
Create a customer.

**Request**
```json
{
  "firstName": "Johnathan",
  "lastName": "Doe",
  "email": "johnathan@example.com",
  "phone": "9876500000"
}
```
**Response `200`**
```json
{
  "id": 1,
  "firstName": "Johnathan",
  "lastName": "Doe",
  "email": "johnathan@example.com",
  "phone": "9876500000",
  "createdAt": "2026-08-17T11:00:00"
}
```

## GET `/API/customer/v1/GetAllCustomerDetails`
List all customers.

**Response `200`**
```json
[
  { "id": 1, "firstName": "Johnathan", "lastName": "Doe", "email": "johnathan@example.com", "phone": "9876500000", "createdAt": "2026-08-17T11:00:00" }
]
```

## GET `/API/customer/v1/GetCustomerById/{id}`
Get a single customer (no addresses).

**Response `200`**
```json
{ "id": 1, "firstName": "Johnathan", "lastName": "Doe", "email": "johnathan@example.com", "phone": "9876500000", "createdAt": "2026-08-17T11:00:00" }
```

## GET `/API/customer/v1/GetCustomerDetailsById/{customer_id}/Addresses`
Get a customer along with all their addresses.

**Response `200`**
```json
{
  "id": 1,
  "firstName": "Johnathan",
  "lastName": "Doe",
  "email": "johnathan@example.com",
  "phone": "9876500000",
  "addresses": [
    {
      "id": 1,
      "addressLine1": "221B Baker Street",
      "addressLine2": "Near Regents Park",
      "city": "Hyderabad",
      "state": "Telangana",
      "postalCode": "500081",
      "addressType": "HOME"
    }
  ]
}
```

## GET `/API/customer/v1/GetCustomerDetailsById/{customer_id}/Addresses/{address_id}`
Get a specific address of a customer.

**Response `200`**
```json
{
  "id": 1,
  "addressLine1": "221B Baker Street",
  "addressLine2": "Near Regents Park",
  "city": "Hyderabad",
  "state": "Telangana",
  "postalCode": "500081",
  "addressType": "HOME"
}
```

## POST `/API/customer/v1/CreateAddressDetails/{Customerid}/Address`
Add an address for a customer.

**Request**
```json
{
  "customerId": 1,
  "addressLine1": "221B Baker Street",
  "addressLine2": "Near Regents Park",
  "city": "Hyderabad",
  "state": "Telangana",
  "postalCode": "500081",
  "addressType": "HOME"
}
```
**Response `200`**
```json
{
  "id": 1,
  "customerId": 1,
  "addressLine1": "221B Baker Street",
  "addressLine2": "Near Regents Park",
  "city": "Hyderabad",
  "state": "Telangana",
  "postalCode": "500081",
  "addressType": "HOME"
}
```

## PUT `/API/customer/v1/UpdateFullCustomerDetails/{id}`
Replace a customer's details.

**Request**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "9876511111"
}
```
**Response `200`** — updated `CustomerDetails`.

## DELETE `/API/customer/v1/DeleteCustomerDetails/{id}`
**Response `200`**: `Customer deleted successfully`

## DELETE `/API/customer/v1/DeleteAddressByCustomerId/{customer_id}/addresses/{address_id}`
**Response `200`**: `Address deleted successfully`

---

# 2. Resturant (`http://localhost:9191`)

## POST `/API/resturant/v1/AddResturntdetails`
Create a restaurant. Status is automatically set to `OPEN`.

**Request**
```json
{
  "resturantName": "Harsh Resturant",
  "description": "Multi-cuisine family restaurant",
  "phone": "9998887777",
  "email": "harsh@example.com",
  "address": "MG Road",
  "city": "Hyderabad",
  "openingTime": "09:00:00",
  "closingTime": "23:00:00"
}
```
**Response `200`**
```json
{
  "id": 1,
  "resturantName": "Harsh Resturant",
  "description": "Multi-cuisine family restaurant",
  "phone": "9998887777",
  "email": "harsh@example.com",
  "address": "MG Road",
  "city": "Hyderabad",
  "openingTime": "09:00:00",
  "closingTime": "23:00:00",
  "status": "OPEN",
  "createdAt": "2026-08-17T11:05:00",
  "updatedAt": "2026-08-17T11:05:00"
}
```

## GET `/API/resturant/v1/GetAllResturnentdetails`
**Response `200`**: array of `ResturantDetails`.

## GET `/API/resturant/v1/GetResturnentdetailsById/{id}`
Get restaurant details **with its menu items** (menu fetched from MenuItemsService via Feign).

**Response `200`**
```json
{
  "id": 1,
  "resturantName": "Harsh Resturant",
  "description": "Multi-cuisine family restaurant",
  "phone": "9998887777",
  "email": "harsh@example.com",
  "address": "MG Road",
  "city": "Hyderabad",
  "openingTime": "09:00:00",
  "closingTime": "23:00:00",
  "status": "OPEN",
  "menuItems": [
    { "id": 1, "name": "Margherita Pizza - Large", "description": "Large classic cheese pizza", "price": 300.0, "category": "Pizza", "vegNonVeg": "Veg", "resturantId": 1 }
  ]
}
```

## PUT `/API/resturant/v1/UpdateResturnentDetailsByID/{id}`
Full update of a restaurant. **Request**: full `ResturantDetails`. **Response `200`**: updated entity.

## PATCH `/API/resturant/v1/UpdateResturnentDetailsByID/{id}`
Partial update (only send fields you want to change).

**Request**
```json
{ "status": "OPEN" }
```
**Response `200`**: updated `ResturantDetails`.

## DELETE `/API/resturant/v1/DeleteResturentDetailsById/{id}`
**Response `200`**: `Resturant deleted successfully`

> **Note:** `status` is an enum — one of `OPEN`, `CLOSED`, `TEMPORARILY_CLOSED`. Orders can only be placed against an `OPEN` restaurant.

---

# 3. MenuItemsService (`http://localhost:9292`)

## POST `/API/menuitems/v1/saveMenuItems`
Create a menu item.

**Request**
```json
{
  "name": "Margherita Pizza - Large",
  "description": "Large classic cheese pizza",
  "price": 300.0,
  "category": "Pizza",
  "vegNonVeg": "Veg",
  "resturantId": 1
}
```
**Response `200`**
```json
{
  "id": 1,
  "name": "Margherita Pizza - Large",
  "description": "Large classic cheese pizza",
  "price": 300.0,
  "category": "Pizza",
  "vegNonVeg": "Veg",
  "resturantId": 1
}
```

## GET `/API/menuitems/v1/GetAllFooditems`
**Response `200`**: array of menu items.

## GET `/API/menuitems/v1/GetFooditemsById/{ID}`
Get a menu item with its restaurant details.

**Response `200`**
```json
{
  "id": 1,
  "name": "Margherita Pizza - Large",
  "description": "Large classic cheese pizza",
  "price": 300.0,
  "category": "Pizza",
  "vegNonVeg": "Veg",
  "resturant": { "id": 1, "resturantName": "Harsh Resturant", "status": "OPEN" }
}
```

## GET `/API/menuitems/v1/GetMenuItemsByResturant/{resturantId}`
All menu items for a restaurant.

**Response `200`**: array of `MenuItems`.

## GET `/API/menuitems/v1/GetByResturant/{resturantId}?category=Pizza&veg=true`
Filtered menu items. Query params `category` and `veg` are optional.

**Response `200`**: filtered array of `MenuItems`.

## PUT `/API/menuitems/v1/updateMenuitemsById/{ID}`
Full update. **Request**: full `MenuItems`. **Response `200`**: updated entity.

## PATCH `/API/menuitems/v1/PatchMenuitemsById/{Id}`
Partial update.

**Request**
```json
{ "price": 320.0 }
```
**Response `200`**: updated `MenuItems`.

## DELETE `/API/menuitems/v1/deleteMenuitemsById/{ID}`
**Response `200`**: `Menuitem deleted successfully`

---

# 4. Order_Service (`http://localhost:9294`)

## POST `/API/Order/v1/CreateOrder`
Create an order. Validates customer, address, restaurant (`OPEN`) and menu items via Feign, then computes prices.

**Request** — send only IDs + items:
```json
{
  "customerId": 1,
  "restaurantId": 1,
  "deliveryAddressId": 1,
  "items": [
    { "menuItemId": 1, "quantity": 2 },
    { "menuItemId": 3, "quantity": 1 }
  ]
}
```
**Response `200`**
```json
{
  "orderId": 1,
  "customerDetails": {
    "id": 1,
    "firstName": "Johnathan",
    "lastName": "Doe",
    "email": "johnathan@example.com",
    "phone": "9876500000",
    "address": {
      "id": 1,
      "addressLine1": "221B Baker Street",
      "addressLine2": "Near Regents Park",
      "city": "Hyderabad",
      "state": "Telangana",
      "postalCode": "500081",
      "addressType": "HOME"
    }
  },
  "restaurantDetails": { "id": 1, "resturantName": "Harsh Resturant", "status": "OPEN" },
  "items": [
    { "id": 1, "name": "Margherita Pizza - Large", "description": "Large classic cheese pizza", "price": 300.0, "category": "Pizza", "vegNonVeg": "Veg" },
    { "id": 3, "name": "Margherita Biryani", "description": "Classic cheese Biryani", "price": 250.0, "category": "Biryani", "vegNonVeg": "Non-Veg" }
  ],
  "status": "CREATED",
  "subtotal": 850.00,
  "tax": 42.50,
  "deliveryFee": 40,
  "totalAmount": 932.50,
  "paymentStatus": "PENDING",
  "createdAt": "2026-08-17T11:24:11",
  "updatedAt": "2026-08-17T11:24:11"
}
```
**Price rules:** `subtotal = Σ(price × qty)`, `tax = 5% of subtotal`, `deliveryFee = 40 (flat)`, `totalAmount = subtotal + tax + deliveryFee`.

## GET `/API/Order/v1/GetOrderById/{OrderId}`
Fetch an order (enriched). **Response `200`**: same shape as create.

## GET `/API/Order/v1/GetAllOrders`
**Response `200`**: array of `OrderResponse`.

## GET `/API/Order/v1/GetAllOrdersByCustomerId/{CustomerId}`
All orders for a customer.

**Response `200`**
```json
{
  "customerId": 1,
  "customerName": "Johnathan Doe",
  "orders": [ { "orderId": 1, "status": "CREATED", "totalAmount": 932.50 } ]
}
```

## GET `/API/Order/v1/GetAllOrdersByResturantId/{ResturantId}`
All orders for a restaurant.

**Response `200`**
```json
{
  "restaurantId": 1,
  "restaurantName": "Harsh Resturant",
  "orders": [ { "orderId": 1, "status": "CREATED", "totalAmount": 932.50 } ]
}
```

## PATCH `/API/Order/v1/orders/{orderId}/status`
Update an order's status.

**Request** (raw string body)
```
CONFIRMED
```
**Response `200`**: updated `OrderResponse`.

## POST `/API/Order/v1/orders/{orderId}/cancel`
Cancel an order. **Response `200`**: `OrderResponse` with `status: CANCELLED`.

## GET `/API/Order/v1/orders/{orderId}/Currentstatus`
Get just the current status.

**Response `200`**
```json
{ "orderId": 1, "status": "CREATED" }
```

---

# 5. Payment-Service (`http://localhost:9295`)

## POST `/API/Payments/v1/payment`
Create a payment. Validates the customer via Feign, generates a transaction id.

**Request**
```json
{
  "orderId": 1,
  "customerId": 1,
  "amount": 932.50,
  "paymentMethod": "CARD",
  "status": "SUCCESS"
}
```
**Response `200`**
```json
{
  "id": 1,
  "orderId": 1,
  "customerId": 1,
  "amount": 932.50,
  "paymentMethod": "CARD",
  "status": "SUCCESS",
  "transactionId": "TXN-3f2a9c8e-7b1d-4a55-9c0e-2b6f8d1e4a77",
  "createdAt": "2026-08-17T11:30:00"
}
```

## GET `/API/Payments/v1/payment/{id}`
Get a payment by its payment id.

**Response `200`**
```json
{
  "id": 1,
  "orderId": 1,
  "customerId": 1,
  "amount": 932.50,
  "paymentMethod": "CARD",
  "status": "SUCCESS",
  "transactionId": "TXN-3f2a9c8e-7b1d-4a55-9c0e-2b6f8d1e4a77",
  "createdAt": "2026-08-17T11:30:00"
}
```

## GET `/API/Payments/v1/order/{orderId}`
Get all payments for an order (array — an order may have a payment and a later refund).

**Response `200`**
```json
[
  {
    "id": 1,
    "orderId": 1,
    "customerId": 1,
    "amount": 932.50,
    "paymentMethod": "CARD",
    "status": "SUCCESS",
    "transactionId": "TXN-3f2a9c8e-7b1d-4a55-9c0e-2b6f8d1e4a77",
    "createdAt": "2026-08-17T11:30:00"
  }
]
```

## POST `/API/Payments/v1/{paymentId}/refund`
Refund a payment. Only a payment currently in `SUCCESS` can be refunded.

**Response `200`**
```json
{
  "id": 1,
  "orderId": 1,
  "customerId": 1,
  "amount": 932.50,
  "paymentMethod": "CARD",
  "status": "REFUNDED",
  "transactionId": "TXN-3f2a9c8e-7b1d-4a55-9c0e-2b6f8d1e4a77",
  "createdAt": "2026-08-17T11:30:00"
}
```

---

# Common Error Response

Failed validations currently surface as `500` with this shape:
```json
{
  "timestamp": "2026-08-17T19:33:53.885Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Restaurant is not OPEN",
  "path": "/API/Order/v1/CreateOrder"
}
```

Common messages:

| Message | Cause |
|---|---|
| `Restaurant is not OPEN` | Ordering from a restaurant whose `status` != `OPEN` |
| `Order not found` | Invalid `orderId` |
| `Payment not found with id: {id}` | Invalid `paymentId` |
| `Only SUCCESS payments can be refunded...` | Refund attempted on a non-SUCCESS payment |

---

# Typical End-to-End Flow

1. **Create customer** → `POST /API/customer/v1/CreateCustomer`
2. **Add address** → `POST /API/customer/v1/CreateAddressDetails/{id}/Address`
3. **Create restaurant** → `POST /API/resturant/v1/AddResturntdetails` (auto `OPEN`)
4. **Add menu items** → `POST /API/menuitems/v1/saveMenuItems`
5. **Create order** → `POST /API/Order/v1/CreateOrder`
6. **Pay** → `POST /API/Payments/v1/payment`
7. **Track / cancel / refund** as needed.
