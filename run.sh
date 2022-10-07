rm -rf ./bin/*.class
cd ./SourceCode
javac -d ../bin ./Application.java
cd ../bin
java Application $1 $2
