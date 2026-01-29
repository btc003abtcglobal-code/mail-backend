# Run Instructions

## Prerequisites
The application requires **Java 17** (JDK) to run, which is currently missing from your system.

### 1. Install Java 17
Open your terminal and run:
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

### 2. Verify Installation
Check that Java is installed correctly:
```bash
java -version
```
It should show `openjdk version "17..."`.

## Running the Application
Once Java is installed, you can start the application:

```bash
cd /media/pradeep/Windows/mail/MailApplication-Backend
./mvnw spring-boot:run
```

## Mail Server Verification
Your local mail servers are already running and configured correctly:
- **Postfix (SMTP)**: Port 25
- **Dovecot (IMAP)**: Port 143

The application is configured to connect to these local servers automatically.
