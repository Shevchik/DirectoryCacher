# DirectoryCacher
Locks files in directories in linux cache to reduce IO usage

Requirements:<br>
1) 64bit linux<br>
2) Java 8

You need to run this programm as root

You can supply list of directories that should be cached either by:<br>
1) Supplying them as command line args<br>
Exmaple: sudo java -jar DirectoryCacher.jar /home/test /home/test1<br>
2) Adding them to dirlist file (located in the process workdir (by default the directory from which you launched java))

DirectoryCacher is recursive so any directory inside the specified ones will be cached too