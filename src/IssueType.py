import re
import base64
import pandas
import csv
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("--cp", type=str, default=None)  # commit_ids.txt
parser.add_argument("--lp", type=str, default=None)  # log.txt
parser.add_argument("--ip", type=str, default=None)  # issueinfo.txt

args = parser.parse_args()

# 从 commit_ids.txt 文件中读取 commit ID 列表
with open(args.cp, 'r') as f: #r'D:\Metric-tool\myproject\atlas\commit_ids.txt'
    commit_ids = [line.strip() for line in f.readlines()]

# 读取 log.txt
with open(args.lp, 'r', encoding='utf-8') as f: #r'D:\Metric-tool\myproject\atlas\log.txt'
    log_lines = f.readlines()

# 从 jira_issue.txt 文件中读取 issue 信息
with open(args.ip, 'r', encoding='utf-8') as f:
    issue_mapping = {}
    for line in f:
        match = re.match(r'Issue Key: (\S+), Type: (\S+), Summary: (.+)', line)
        if match:
            issue_key, issue_type, summary = match.groups()
            issue_mapping[issue_key.strip()] = issue_type.strip()
# 将结果输出到 CSV 文件
output_file = 'IssueType.csv'
with open(output_file, 'w', newline='', encoding='utf-8') as csvfile:
    fieldnames = ['Commit ID', 'Issue Name', 'Issue Type']
    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
    writer.writeheader()
    # 查找每个 commit 对应的 issue 类型
    commit_issue_info = []
    for commit_id in commit_ids:
        current_commit_message = ''
        found_commit = False
        for line in log_lines:
            if line.startswith(f'commit {commit_id}'):
                found_commit = True
            elif found_commit and line.startswith('commit '):
                break  # 如果找到下一个 commit，说明当前 commit 处理完毕
            elif found_commit:
                current_commit_message += line

        if current_commit_message:
            issue_number_match = re.search(r'([A-Za-z]+-\d+)', current_commit_message)
            if issue_number_match:
                issue_name = issue_number_match.group()
                issue_type = issue_mapping.get(issue_name, 'Unknown')
                writer.writerow({'Commit ID': commit_id, 'Issue Name': issue_name, 'Issue Type': issue_type})
            else:
                writer.writerow({'Commit ID': commit_id, 'Issue Name': '/', 'Issue Type': '/'})
        else:
                # 如果找不到对应的 Issue，填入 "/"
                writer.writerow({'Commit ID': commit_id, 'Issue Name': '/', 'Issue Type': '/'})




