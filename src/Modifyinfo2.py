import lizard
import git
from pydriller import Repository, Git
import pandas as pd
import csv
import os
import argparse

def get_lastinfo(GitPath, ProjectPath, FileInfo, gr, LastcommitID, commitID, OldName, FileList,FileType, FirstFileLine, LastFileComplexityInfo, LastFileTokenCountInfo, LastFileMethodNumInfo):
    gr.checkout(LastcommitID)
    CommitComplexity = 0
    CommitToken = 0
    CommitMethodNum = 0

    i = 0
    for FileName in FileList:
        print("FileName:" + FileName)
        if FileType[i] == 'A':#下一个提交增加的文件
            i = i + 1
            LastFileComplexityInfo.append(0)
            LastFileTokenCountInfo.append(0)
            LastFileMethodNumInfo.append(0)
            continue
        if FileType[i] == 'R':#改名的文件
            # FileComplexity, FileToken, FileMethodNum = get_metrics_for_file(GitPath, commitID, OldName)
            info = lizard.analyze_file(ProjectPath + "\\" + OldName)

        else:
            # FileComplexity, FileToken, FileMethodNum = get_metrics_for_file(GitPath, commitID, FileName)
            info = lizard.analyze_file(ProjectPath + "\\" + FileName)
        FileComplexity = 0
        FileToken = info.token_count
        FileMethodNum = len(info.function_list)

        for function in info.function_list:
            FileComplexity = FileComplexity + function.cyclomatic_complexity

        CommitComplexity = CommitComplexity + FileComplexity
        CommitToken = CommitToken + FileToken
        CommitMethodNum = CommitMethodNum + FileMethodNum

        LastFileComplexityInfo.append(FileComplexity)
        LastFileTokenCountInfo.append(FileToken)
        LastFileMethodNumInfo.append(FileMethodNum)
        i = i + 1

    FileInfo.loc[FirstFileLine, 'LastCommitComplexity'] = CommitComplexity
    FileInfo.loc[FirstFileLine, 'LastCommitTokenCount'] = CommitToken
    FileInfo.loc[FirstFileLine, 'LastCommitMethodNum'] = CommitMethodNum

def get_info(GitPath, ProjectPath, FileInfo, FileNum, gr, commitID, LastCommitID, index,FileComplexityInfo, FileTokenCountInfo, FileMethodNumInfo, LastFileComplexityInfo, LastFileTokenCountInfo, LastFileMethodNumInfo):
    print(commitID)
    repo = git.Repo(GitPath)
    commit1 = repo.commit(LastCommitID)
    commit2 = repo.commit(commitID)

    diff_index = commit1.diff(commit2)

    OldName = ''

    i = 0
    #本提交的圈复杂度等信息
    gr.checkout(commitID)
    FileList = []
    #记录文件类型 1--正常修改文件 2--增加的文件 3--删除的文件
    FileType = []
    #print(commitID)
    CommitComplexity = 0
    CommitToken = 0
    CommitMethodNum = 0
    FirstFileLine = -1
    HasFixFile = 0
    isDelete = 0
    while(i < FileNum):
        #print(FileInfo['FileName'][index])
        #print('oooooooooooooooo')
        isDelete = 0
        FileName = FileInfo['FileName'][index]
        FileList.append(FileName)
        HasType = 0
        #是增加或者删除的文件 信息填-1并跳过
        #删除文件 没有this 但是有last
        for diff in diff_index:
            file_name = diff.b_path if diff.b_path else diff.a_path
            if file_name == FileName:
                if diff.deleted_file:
                    FileType.append("D")
                    i = i + 1
                    # FileInfo = FileInfo.drop(index,axis = 0)
                    # print(FileInfo)
                    FileComplexityInfo.append(0)
                    FileTokenCountInfo.append(0)
                    FileMethodNumInfo.append(0)
                    index = index + 1
                    isDelete = 1
                    break
                elif diff.new_file:
                    FileType.append("A")
                    HasType = 1
                elif diff.renamed:
                    FileType.append("R")
                    OldName = diff.a_path
                    HasType = 1
                else:
                    FileType.append("M")
                    HasType = 1

        if isDelete == 1:
            continue

        if HasType == 0:
            FileType.append("M")

        if FirstFileLine == -1:
            FirstFileLine = index
            HasFixFile = 1
        # FileComplexity, FileToken, FileMethodNum = get_metrics_for_file(GitPath, commitID, OldName)
        info = lizard.analyze_file(ProjectPath + "\\" + FileName)

        FileComplexity = 0
        FileToken = info.token_count
        FileMethodNum = len(info.function_list)



        for function in info.function_list:
            FileComplexity = FileComplexity + function.cyclomatic_complexity

        CommitComplexity = CommitComplexity + FileComplexity
        CommitToken = CommitToken + FileToken
        CommitMethodNum = CommitMethodNum + FileMethodNum

        FileComplexityInfo.append(FileComplexity)
        FileTokenCountInfo.append(FileToken)
        FileMethodNumInfo.append(FileMethodNum)
        index = index + 1
        i = i + 1

    if i == 0:
        FirstFileLine = index
        FileComplexity = 0
        FileToken = 0
        FileMethodNum = 0
        FileComplexityInfo.append(FileComplexity)
        FileTokenCountInfo.append(FileToken)
        FileMethodNumInfo.append(FileMethodNum)
        index = index + 1
        HasFixFile = 1

    if HasFixFile == 0:
        FirstFileLine = index - 1


    FileInfo.loc[FirstFileLine,'CommitComplexity'] = CommitComplexity
    FileInfo.loc[FirstFileLine,'CommitTokenCount'] = CommitToken
    FileInfo.loc[FirstFileLine,'CommitMethodNum'] = CommitMethodNum
    #文件列表为空
    if len(FileList) == 0:
        m = index - FirstFileLine
        n = 0
        while(n < m):
            LastFileComplexityInfo.append(0)
            LastFileTokenCountInfo.append(0)
            LastFileMethodNumInfo.append(0)
            FileInfo.loc[FirstFileLine, 'LastCommitComplexity'] = 0
            FileInfo.loc[FirstFileLine, 'LastCommitTokenCount'] = 0
            FileInfo.loc[FirstFileLine, 'LastCommitMethodNum'] = 0

            FileInfo.loc[FirstFileLine, 'CFC'] = CommitComplexity
            FileInfo.loc[FirstFileLine, 'NCC'] = CommitToken
            FileInfo.loc[FirstFileLine, 'NMC'] = CommitMethodNum
            n = n + 1
    else:
        get_lastinfo(GitPath, ProjectPath, FileInfo, gr, LastCommitID, commitID, OldName, FileList, FileType, FirstFileLine, LastFileComplexityInfo, LastFileTokenCountInfo, LastFileMethodNumInfo)
        FileInfo.loc[FirstFileLine, 'CFC'] = abs(CommitComplexity - FileInfo.loc[FirstFileLine, 'LastCommitComplexity'])
        FileInfo.loc[FirstFileLine, 'NCC'] = abs(CommitToken - FileInfo.loc[FirstFileLine, 'LastCommitTokenCount'])
        FileInfo.loc[FirstFileLine, 'NMC'] = abs(CommitMethodNum - FileInfo.loc[FirstFileLine, 'LastCommitMethodNum'])
    print(len(LastFileComplexityInfo))
    print('***************************')
    return index

def main():

    parser = argparse.ArgumentParser()
    parser.add_argument("--pn", type=str, default=None)  # 项目名
    parser.add_argument("--mp", type=str, default=None)  # map文件地址
    parser.add_argument("--gp", type=str, default=None)  # .git文件地址
    parser.add_argument("--p2", type=str, default=None)  #项目地址 用于分析文件
    parser.add_argument("--m1p", type=str, default=None)  #Metrics1.csv path

    # 获取当前脚本所在的目录
    current_script_directory = os.path.dirname(os.path.realpath(__file__))

    # 假设项目根目录在当前目录的上一级
    out_root = os.path.dirname(current_script_directory)

    args = parser.parse_args()

    ProjectName = args.pn #'chukwa'
    MapPath = args.mp #r'D:\Metric-tool\_5MetricCal\Modifyinfo\chukwa\chukwa_Map.txt'
    GitPath = args.gp #r'D:\Metric-tool\myproject' + '\\' + ProjectName + '\.git'
    ProjectPath = args.p2 #r'D:\Metric-tool\myproject' + '\\' + ProjectName
    FileInfoPath = args.m1p #r'D:\Metric-tool\_5MetricCal\Modifyinfo\chukwa\Metrics1.csv'
    OutPath = out_root + '\\' + ProjectName #r'D:\Metric-tool\_5MetricCal\Modifyinfo' + '\\' + ProjectName

    if not os.path.exists(OutPath):
        os.makedirs(OutPath)

    AllCommitID = []
    CommitOrder = []
    f = open(MapPath)
    line = f.readline()
    # order = 1
    #读取所有CommitID以及序号
    while line:
        content = line.split(':')
        if len(content) > 1:
            commitID = content[1].split('\t')
            commitID = commitID[0]
            commitID = commitID.strip()
            AllCommitID.append(commitID)
            Order = content[2]
            Order = Order.strip()
            CommitOrder.append(Order)
            # order = order + 1
        line = f.readline()
    # print(AllCommitID)
    f.close()
    FileInfo = pd.read_csv(FileInfoPath, encoding='gb18030')
    gr = Git(GitPath)


    index = 0;

    FileComplexityInfo = []
    FileTokenCountInfo = []
    FileMethodNumInfo = []
    LastFileComplexityInfo = []
    LastFileTokenCountInfo = []
    LastFileMethodNumInfo = []


    Size = FileInfo['NMF'].size
    print(Size)
    m = 1
    #开始循环
    while (index < Size):
        #print(FileInfo['FileName'].size)
        commitID = FileInfo['CommitID'][index]
        # commitID = AllCommitID[m]
        LastcommitID = AllCommitID[m - 1]
        #print(commitID)
        #print(LastcommitID)
        FileNum = FileInfo['NMF'][index]
        index = get_info(GitPath, ProjectPath, FileInfo, FileNum, gr, commitID, LastcommitID, index, FileComplexityInfo, FileTokenCountInfo, FileMethodNumInfo, LastFileComplexityInfo, LastFileTokenCountInfo, LastFileMethodNumInfo)
        m = m + 1
        if m < CommitOrder.__len__() and CommitOrder[m] == '1':
            m = m + 1
    #将得到的数组赋值给Dataframe
    FileInfo['FileComplexity'] = FileComplexityInfo
    FileInfo['FileTokenCount'] = FileTokenCountInfo
    FileInfo['FileMethodNum'] = FileMethodNumInfo

    FileInfo['LastFileComplexity'] = LastFileComplexityInfo
    FileInfo['LastFileTokenCount'] = LastFileTokenCountInfo
    FileInfo['LastFileMethodNum'] = LastFileMethodNumInfo




    CommitInfo = FileInfo[['CommitID','FileName', 'FileComplexity', 'FileTokenCount', 'FileMethodNum', 'LastFileComplexity', 'LastFileTokenCount', 'LastFileMethodNum', 'CommitComplexity', 'LastCommitComplexity','CFC', 'CommitTokenCount', 'LastCommitTokenCount','NCC', 'CommitMethodNum', 'LastCommitMethodNum', 'NMC']]
    CommitInfo.to_csv(OutPath + "\\" + "Metrics2.csv", sep=',', index=None)

if __name__ == '__main__':
    main()






