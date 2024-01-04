# CMC
CMC is a _Commit-level Metrics Calculator_ that can calculate metrics are mined from source code and historical data.
![language](https://img.shields.io/badge/language-java-blue) ![language](https://img.shields.io/badge/language-python-green)
--- 
![Metrics calculating process]([https://github.com/Quiet233/CMC/blob/main/Metrics%20calculating%20process.gif](https://github.com/Quiet233/CMC/blob/main/teaser/Metrics%20calculating%20process.gif))

## Metrics
The metrics calculated by this tool are as follows：
|     Type                 |     Feature        |     Definition                                                                                                |   
|--------------------------|--------------------|---------------------------------------------------------------------------------------------------------------|
|     Volume-related       |     NCC            |     Number of characters   changed                                                                            |   
|                          |     LC             |     Lines of code changes                                                                                     |   
|                          |     NMC            |     Number of methods   changed                                                                               |   
|     File-related         |     NMS            |     Number of modified subsystems                                                                             |   
|                          |     NMD            |     Number of modified directories                                                                            |   
|                          |     NMF            |     Number of modified files                                                                                  |   
|                          |     Entropy        |     Distribution of modified code across each file                                                            |   
|                          |     NAD            |     Number of added or deleted files                                                                          |   
|                          |     TLMF           |     The total number of lines of code before the change in modified files                                     |   
|                          |     CFC            |     Cyclomatic complexity   of file changes                                                                   |   
|                          |     NUC            |     The max number of changes to the modified files                                                           |   
|                          |     AGE            |     The average time interval between the last and   current change                                           |   
|     Developer-related    |     NDVE           |     Number of developers                                                                                      |   
|                          |     REXP           |     Recent developer experience                                                                               |   
|                          |     EXP            |     Developer experience                                                                                      |   
|     Issue-related        |     Issue-Type     |     Most of commits are assigned a type to indicate   its purpose (e.g., Bug, Improvement and New Feature)    |   

## 2.Usage
### 1) Set up Java environment and Python environment
you should set up Java environment.(`jdk11`) you should set up `Python3.10` environment.
### 2) Run Modifyinfo.jar
Before running the jar file, you need to set the following `parameters`：
- < ProjectName > Project name, used for folder and file naming
- <CommitIDPath> txt file that stores the `hash codes` of all commits. You can obtain it through the following operations:
```shell
cd yourProjectPath
git log --format="%H" > commit_ids.txt
```

- <gitPath> .git file path in your github project
- <OutPath> Specify the location of the output folder

**Please be sure to use files in the same format as the example, and ensure that they are in the same order, otherwise the results may be inaccurate.**

Example:
```shell
cd out\artifacts\Modifyinfo_jar
java -jar Modifyinfo.jar avro avro\commit_ids.txt avro\.git
```

The results will be output to OutPath\ProjectName\Metrics1.csv

### <div id="jump"> 3) Run Modifyinfo2.py </div>
Enter the CMC project folder，install the `requirements` as the following commands: 
```shell
cd CMC
pip install -r requirements.txt
```
To run a python file from the command line, you need to determine the following parameters：

- --pn: project name
- --p2: your project path
- --gp: .git file path in your github project
- --mp: map file path obtained by running Modifyinfo.jar
- --m1p: Metrics1.csv file path obtained by running Modifyinfo.jar

Example：
```shell
cd src
python Modifyinfo2.py --pn=avro --p2=example\avro --gp=example\avro\.git --mp=Dataset\Map\avro_Map.txt   --m1p=Dataset\Metrics\avro\Metrics1.csv
```

The results will be output to ProjectName\Metrics2.csv

### 4) Get issueType
#### 4.1) First run the IssueInfo.py file
Run the python file by specifying the project name on the command line.（The required installation steps are the same as [`3) Run Modifyinfo2.py`](#jump)）

Example:
```shell
cd src
python IssueInfo.py --pn=avro
```
`If requests.exceptions.ProxyError is prompted, you can try to disconnect the proxy`
The results will be output to src\ProjectName_issueinfo.txt
#### 4.2) Run the IssueType.py file
First use the following git command to obtain the log of the project：
```shell
cd yourProjectPath
git log >log.txt
```
To run a python file from the command line, you need to determine the following parameters：
- --cp: txt file that stores the `hash codes` of all commit, the method to obtain it is introduced in [`2) Run Modifyinfo.jar`](#jump2)
- --lp: log.txt file path
- --ip: issueinfo.txt file path（obtained in the previous step）

Example:
```shell
cd src
python IssueType.py --cp=example\avro\commit_ids.txt --lp=example\avro\log.txt --ip=avro_issueinfo.txt
```


## DataSet
We recovered the software architecture of 30 projects and exist in the data set [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.10444330.svg)](https://doi.org/10.5281/zenodo.10444330). The project information is as follows：
|         System         |                                        Domain                                        |  Commits | Issues |   Files  |    Star    |
|:----------------------:|:------------------------------------------------------------------------------------:|:--------:|:------:|:--------:|:----------:|
|          Atlas         |                         Extensible   set of corefoundational                         |   3,754  |  4,820 |   1,001  |    1,673   |
|          Avro          |                       governance   services Data serialization                       |   3,411  |  3,915 |    716   |    2,648   |
|         Chukwa         |                                  Data   collection                                   |    853   |   825  |    469   |     83     |
|         Crunch         |                                   Java   library                                     |   1,092  |   698  |    730   |     104    |
|        Curator         |                              Java/JVM   client library                               |   2,787  |   694  |    695   |    3,028   |
|         Datafu         |                                Collection of libraries                               |    612   |   173  |    261   |     109    | 
|          Eagle         |                           Open   source analytics solution                           |   1,048  |  1,107 |   1,801  |     408    |
|        Eventmesh       |                             Serverless   event middleware                            |   4,513  |  4,535 |   1,263  |    1,480   |
|         Falcon         |                     Feed   processing and feed manage- 2,224 ment                    |   2,224  |  2,344 |    850   |     102    |
|          Flume         |                                 Log   data processing                                |   2,000  |  3,480 |    635   |    2,472   |
|         Giraph         |                            Large-scale   graph processing                            |   1,134  |  1,255 |   1,516  |     615    |
|          Gora          |                    In-memory   data model and big data persistence                   |   1,419  |   714  |    664   |     111    |
|          Hama          |                                 Big   data analytics                                 |   1,592  |  1,012 |    427   |     131    |
|         inlong         |           One-stop,   full-scenario integration framework for massive data           |   3,922  |   811  |   3,908  |    1,250   |
|          Knox          |                                 Application   Gateway                                |   2,726  |  2,992 |   1,586  |     173    |
|           Kylin(main)  | Open source   Distributed Analyics  Engine                                           |   7,607  |  5,738 |   1,433  |    3,556   |
|         Mahout         |                      Machine   learning application environment.                     |   4,304  |  2,173 |   1,245  |    2,101   |
|         Metron         |                                Big   data technologies                               |   1,452  |  2,363 |   1,344  |     840    |
|          Mina          |                                 Network   application                                |   2,402  |  1,176 |    319   |     886    |
|          Oltu          |                                   OAuth   protocol                                   |    845   |   218  |    247   |     166    |
|         Openjpa        |           Implementation   of the Jakarta Persistence API 3.0 specification          |   5,231  |  2,918 |   4,722  |     124    |
|         Opennlp        |                              Machine   learning toolkit                              |   2,063  |  1,520 |   1,061  |    1,313   |
|           Pig          |                         Dataflow   Programming environ- ment                         |   3,722  |  5,444 |   1,791  |     663    |
|          Ratis         |                               Raft   consensus protocol                              |   1,575  |  1,955 |    632   |    1,112   |
|         Sedona         |                             Cluster   computing framework                            |   1,382  |   438  |    286   |    1,622   |
|         Sentry         |                                Highly   modular system                               |   1,539  |  2,567 |    842   |     120    |
|         Struts         |             Free   open-source solution for creatng Java web applications            |   6,892  |  5,371 |   2,553  |    1,243   |
|        Submarine       |                      Cloud   Native Machine Learning Platform .                      |   1,040  |  1,415 |    411   |     676    |
|          Tiles         |                          Open-sourced   templating framework                         |   1,460  |   600  |    291   |     104    |
|        Zookeeper       | Centralized   service for distributed synchronization, and providing  group services |   2,463  |  4,777 |    919   |   11,680   |
