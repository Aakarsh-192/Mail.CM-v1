
![Mail.CM Logo](CMlogo.png)


# Mail.CM v1

GUI based Java project for Email Client Simulation for GUVI.


## Installation and Startup

Recommended method,  
Download the latest release zip file > Extract > open `start.bat`  
  
Alternate method,  
Installation:
```bash
git clone https://github.com/Aakarsh-192/Mail.CM-v1.git
```
Startup:
```bash
mkdir classes
javac -cp ".:sqlite-jdbc-3.51.0.0.jar" -d classes *.java
java -cp "classes:sqlite-jdbc-3.51.0.0.jar" EmailClient
```
## Features

- Compose
- Inbox
- Sent
- Archive
- Trash
- Multiuser
- JDBC Data Management
- File Data Management
- Account Deletion
- 3 test accounts already added manually (t1/t2/t3@mail.cm:000000) 


## Roadmap

- Better and smoother functionality
- More features
- Implementation of JavaFX for better visuals


## Authors

- [@Aakarsh-192](https://www.github.com/Aakarsh-192) Leader
- [@Adarshraj28](https://www.github.com/Adarshraj28)
- [@ishikasukhija](https://www.github.com/ishikasukhija)

