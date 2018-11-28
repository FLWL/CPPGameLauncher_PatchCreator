# CPPGameLauncher PatchCreator
This tool is used to create the necessary patch files for CPPGameLauncher. It can iterate through all files in a directory, compress them, create checksums, and write manifest files.

# Installation
1. Clone this repository.
2. Open the .iml project file with IntelliJ Idea.
3. Add Apache Commons IO .jar file to the dependencies
4. Set up the directory structure and other constants in the "cpp.game.launcher.Constants" class:
```
"baseDirectory" is the main directory where all others are located in.
"gameDirectory" is a directory inside the base directory where the game files that are to be distributed to the players are located in.
"uploadDirectory" is where the patcher will put ready-made patch files into.
"patchGameVersion" is the version to give to the game files in the patch
"patchLauncherVersion" is the version to distribute the launcher as
```
5. Download or compile OpenSSL binaries, specifically the openssl.exe command line tool is required to generate RSA keypairs. Run the following commands.

Generate a key pair:

```openssl genrsa -out keypair.pem 2048```

Create a private key in .der format for the PatchCreator:

```openssl pkcs8 -topk8 -inform PEM -outform DER -in keypair.pem -out private_key.der -nocrypt```

6. Copy the created "private_key.der" into the patcher's "baseDirectory"
7. Copy your compiled CPPGameLauncher .exe file into the same base directory and name it as "latest_launcher.exe"

# Usage
If you run the program, it should find all files from the "gameDirectory", create a patch out of them, and put the resulting files into "uploadDirectory", which can be uploaded to a web server for the CPPGameLauncher to use.

You can also create a news.txt file in the base directory. The format is as follows (game version number, description, date, link):

```
0.01
Early access first build
25.03.2018
http://www.example.com/

0.02
Footstep sounds, general improvements
02.04.2018
http://www.example.com/
```

First 3 of such entries will be shown in the launcher.

# Patch structure
After creating a patch, the "upload" directory will contain the following:
```
latest.txt - a text file containing information about the latest game and launcher versions
news.txt - news file that the launcher will show on the main page
latest.sig and news.sig - signature files for launcher to confirm the authenticity of those files
latest_launcher.exe - a copy of the CPPGameLauncher's .exe file. The launcher will replace itself with this file if it detects that its current version is less than one specified in latest.txt
```
The folder will also contain a numerically named version folder, where the actual compressed .gz game files are located. Inside that folder there is a manifest file called "checksums.txt", which contains the information about all game files that are to be downloaded, including CRC hashes, filesizes, etc. Likewise this file is accompanied with "checksums.sig" for verification.
