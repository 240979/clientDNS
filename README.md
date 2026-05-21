# Client DNS
GUI for DNS Client. This repository contains code from:

https://github.com/mbio16/clientDNS

https://github.com/xramos00/DNS_client

## Setup
- JAVA version 25
- JAVAFX SDK version 25
- GLUON Scene Builder
- IntelliJ IDEA Community Edition 2025.1.3

## Import
- Import project into IDE
- Run configuration  
- Main: application.App

## Deployment
- IDE: 
  - Maven clean 
  - Maven package
- .exe is located in ./target/clientDNS/clientDNS.exe
  - if build on Windows
- .sh is located in ./target/clientDNS/clientDNS.sh
  - if build on Linux
- Or using GitHub Workflows:
  - ``git tag -a v<version_number> -m "<message>"``
  - ``git push origin v<version_number>``
  - This creates release with both Windows and Linux apps.

## Convention

Classes - Starts With Cap. Letter, no spaces **MainClass**

variables - starts with small letter ex. **smallNumber**

CONSTANT - All letters Cap. and spaces are _ ex. **SPECIAL_CONST**
