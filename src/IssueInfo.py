import os.path
import sys
sys.path.append(r'D:\Python\Lib\site-packages')
import requests
import argparse

jira_base_url = 'https://issues.apache.org/jira/rest/api/2/'

parser = argparse.ArgumentParser()
parser.add_argument("--pn", type=str, default=None)  # 项目名

# 获取当前脚本所在的目录
current_script_directory = os.path.dirname(os.path.realpath(__file__))

# 假设项目根目录在当前目录的上一级
out_root = os.path.dirname(current_script_directory)

args = parser.parse_args()
# 替换为您的项目关键字
project_key = args.pn

# 存储输出的文件路径
output_file_path = project_key + '_issueinfo.txt'

# 发送请求并获取响应
response = requests.get(f'{jira_base_url}search?jql=project={project_key}&maxResults=1000')

# 打开文件以写入输出
with open(output_file_path, 'w', encoding='utf-8') as output_file:
    # 检查请求是否成功
    if response.status_code == 200:
        # 解析 JSON 响应
        data = response.json()

        # 提取 issue 信息
        issues = data.get('issues', [])

        # 遍历每个 issue
        for issue in issues:
            issue_key = issue.get('key')
            issue_type = issue['fields']['issuetype']['name']
            issue_summary = issue['fields']['summary']

            # 写入 issue 信息到文件
            output_file.write(f"Issue Key: {issue_key}, Type: {issue_type}, Summary: {issue_summary}\n")

        # 获取总结果数
        total_results = data.get('total', 0)
        i = 1
        start_at = i * 1000
        # 循环获取余下的结果
        while start_at < total_results:
            #start_at = len(issues)
            api_url = f'{jira_base_url}search?jql=project={project_key}&startAt={start_at}&maxResults=1000'
            response = requests.get(api_url)

            # 检查请求是否成功
            if response.status_code == 200:
                # 解析 JSON 响应
                data = response.json()

                # 提取 issue 信息
                issues = data.get('issues', [])

                # 遍历每个 issue
                for issue in issues:
                    issue_key = issue.get('key')
                    issue_type = issue['fields']['issuetype']['name']
                    issue_summary = issue['fields']['summary']

                    # 写入 issue 信息到文件
                    output_file.write(f"Issue Key: {issue_key}, Type: {issue_type}, Summary: {issue_summary}\n")

                if not issues:
                    # 如果没有更多的问题，退出循环
                    break
            else:
                print(f"Failed to retrieve additional issues. Status Code: {response.status_code}")
                break
            i = i + 1
            start_at = i * 1000

    else:
        print(f"Failed to retrieve issues. Status Code: {response.status_code}")