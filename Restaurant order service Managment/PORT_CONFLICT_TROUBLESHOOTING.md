# Port Conflict Troubleshooting for Spring Boot Services

## Problem

When a Spring Boot service fails to start with an error like:

```text
Web server failed to start. Port 9999 was already in use.
```

or:

```text
Web server failed to start. Port 9294 was already in use.
```

it means another process is already listening on that port.

This is a common issue when:

- a previous instance of the service is still running
- another app is using the same port
- a stopped app left a socket in a short-lived state

---

## Important note

`BUILD SUCCESS` means the project compiled successfully.
It does not mean the Spring Boot application started successfully.

The actual app startup fails when Tomcat tries to bind to the port and finds it occupied.

---

## Step 1: Check which process is using the port

Open PowerShell and run:

```powershell
netstat -ano | findstr :9999
```

Replace `9999` with the port shown in the error.

Typical output:

```text
TCP    0.0.0.0:9999    0.0.0.0:0    LISTENING    12345
```

The last number (`12345`) is the PID.

---

## Step 2: Identify the process

```powershell
tasklist | findstr 12345
```

If it is an old Java or Spring Boot process, stop it.

---

## Step 3: Stop the old process

```powershell
taskkill /PID 12345 /F
```

Or:

```powershell
Stop-Process -Id 12345 -Force
```

---

## Step 4: Verify the port is free

```powershell
Get-NetTCPConnection -LocalPort 9999 -ErrorAction SilentlyContinue
```

If there is no output, the port is free and the application can start normally.

---

## Step 5: Start the service again

From the service folder:

```powershell
./mvnw.cmd spring-boot:run
```

or start the application from your IDE.

You should see logs similar to:

```text
Tomcat initialized with port 9999 (http)
Tomcat started on port 9999
Started OrderServiceApplication
```

---

## Alternative fix: change the port

If another application really needs that port, change the service port in:

```text
src/main/resources/application.properties
```

Example:

```properties
server.port=9998
```

Then update any other service calling it to the new port.

---

## Example fix used for this project

The Order service originally failed because it was trying to bind to port `9294`, which was already in use.

The app was moved to a free port:

```properties
server.port=9298
```

And the Payment service Feign client was updated to use:

```text
http://localhost:9298
```

This resolved the startup issue.

---

## Quick checklist

If you see this error again, do this in order:

1. Run `netstat -ano | findstr :<PORT>`
2. Find the PID
3. Stop the process with `taskkill` or `Stop-Process`
4. Verify the port is free
5. Start the Spring Boot app again
6. If needed, switch to a different port in `application.properties`

---

## Summary

This issue is not a MySQL problem and not a Java compilation problem.
It is a port binding problem in Spring Boot.

If the service keeps failing with `Port ... was already in use`, check the port first and free or reassign it before changing database or Kafka settings.
