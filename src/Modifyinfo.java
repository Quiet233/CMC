import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import org.eclipse.jgit.lib.ObjectLoader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.io.*;
import java.util.*;
import java.net.URL;

public class Modifyinfo {
    public static void main(String[] args) throws IOException {
        String ProjectName =  args[0];//"chukwa";
        String CommitIDPath = args[1];//"D:\\Metric-tool\\myproject\\" + ProjectName + "\\commit_ids.txt";
        String Gitpath = args[2];//"D:\\Metric-tool\\myproject\\" + ProjectName + "\\.git";
        String OutRoot = args[3];//"D:\\Metric-tool\\";
        String Outpath = OutRoot + "\\"+ ProjectName;
        File file = new File(Outpath);
        if(!file.exists())
            file.mkdirs();
        //将commitID和顺序输出到map文件中
        LinkedHashMap<String, Integer> H = new LinkedHashMap<>();//存hash码
        H = GetID(CommitIDPath);
        String MapTxt = Outpath + "\\" + ProjectName + "_Map.txt";
        File Fm = new File(MapTxt);
        if (Fm.exists())
            Fm.delete();
        for (String key : H.keySet()) {
            PrintStream p = new PrintStream(new FileOutputStream((MapTxt), true));
            System.setOut(p);
            System.out.println("Key : " + key + "\t\t"
                    + "Value : "
                    + H.get(key));
        }
        LinkedHashMap<Integer, String> HashCode = new LinkedHashMap<>();//存放所有commit的hash码
        List<Integer> CommitNumber = new ArrayList<>();//记录所有KEY
        //通过map得到所有commitID
        GetHashCode(HashCode, CommitNumber, MapTxt);



        String outpath1 = Outpath + "\\Metrics1.csv";



        CommitFileInfo(ProjectName, HashCode, CommitNumber, outpath1, Gitpath);
    }
    public static LinkedHashMap<String, Integer> GetID(String CommitIDPath) {
        LinkedHashMap<String, Integer> H = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(CommitIDPath))) {
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                H.put(line, lineNumber);
                lineNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 将 LinkedHashMap 的内容逆序
        LinkedHashMap<String, Integer> reversedMap = new LinkedHashMap<>();

        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(H.entrySet());
        Collections.reverse(entryList);

        for (Map.Entry<String, Integer> entry : entryList) {
            reversedMap.put(entry.getKey(), entry.getValue());
        }
        //将value逆序
        for (String sr : reversedMap.keySet() ) {
            reversedMap.put(sr, reversedMap.size() - reversedMap.get(sr) + 1);
        }
        // 输出逆序的reverseHashMap
//        System.out.println("\nReversed LinkedHashMap:");
//        for (String key : reversedMap.keySet()) {
//            System.out.println(key + ": " + reversedMap.get(key));
//        }
        return reversedMap;
    }
    public static LinkedHashMap<String, Integer> getJavaFileCodeLines(String repositoryPath, String commitID) throws IOException {
        LinkedHashMap<String, Integer> javaFileCodeLines = new LinkedHashMap<>();

        try (Git git = Git.open(new java.io.File(repositoryPath)) ) {
            Repository repository = git.getRepository();
            RevCommit commit = getCommit(repository, commitID);
            RevTree tree = commit.getTree();
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                String fileName = treeWalk.getPathString();
                if (fileName.endsWith(".java")) {
                    int codeLines = getCodeLines(repository, commitID, fileName);
                    javaFileCodeLines.put(fileName, codeLines);
                }
            }
        }

        return javaFileCodeLines;
    }
    private static RevCommit getCommit(Repository repository, String commitID) throws IOException {
        ObjectId objectId = repository.resolve(commitID);
        try (RevWalk revWalk = new RevWalk(repository)) {
            return revWalk.parseCommit(objectId);
        }
    }

    private static int getCodeLines(Repository repository, String commitID, String fileName) throws IOException {
        ObjectId objectId = repository.resolve(commitID);
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(objectId);
            RevTree tree = commit.getTree();
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(fileName));

                if (!treeWalk.next()) {
                    return 0; // 文件不存在
                }

                ObjectId fileObjectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(fileObjectId);
                byte[] bytes = loader.getBytes();

                String fileContent = new String(bytes);
                // 使用正则表达式排除多行注释块
                //fileContent = fileContent.replaceAll("/\\*([^*]|(\\*+[^*/]))*\\*+/", "");
                int start = fileContent.indexOf("/**");
                while (start >= 0) {
                    int end = fileContent.indexOf("*/", start);
                    if (end >= 0) {
                        fileContent = fileContent.substring(0, start) + fileContent.substring(end + 2);
                        start = fileContent.indexOf("/**");
                    } else {
                        // 处理注释未关闭的情况
                        break;
                    }
                }

                String[] lines = fileContent.split("\\r?\\n");

                int codeLines = 0;

                for (String line : lines) {
                    // 假设你希望排除空行和注释行，你可以自行定义判断条件
                    if (!line.trim().isEmpty() && !line.trim().startsWith("//")) {
                        codeLines++;
                    }
                }

                return codeLines;
            }
        }
    }

    public static void CommitFileInfo(String ProjectName, LinkedHashMap<Integer, String > HashCode, List<Integer> CommitNumber, String OutPath, String Gitpath) throws IOException {
        LinkedHashMap<String, Integer> fc = new LinkedHashMap<>();//存放文件代码行数
        /******文件增减 总代码行数等指标输出文件****/
        FileOutputStream fileOutputStream = new FileOutputStream(OutPath, true);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "GBK");
        BufferedWriter bw1 = new BufferedWriter(outputStreamWriter);
        /**************文件依赖数输出文件*****/
        bw1.write("CommitID,NMF,TotalAddLines,TotalDeleteLines,LC,FileName,AddLines,DeleteLines,CodeLines,FileAdd,FileDelete,NAD,NDEV,NUC,AGE,NS,ND,Entropy,TLMF,EXP,REXP");
        bw1.newLine();
        bw1.flush();

        //每一个提交
        for (int i = 1; i < CommitNumber.size(); i++) {
            fc = getJavaFileCodeLines(Gitpath, HashCode.get(CommitNumber.get(i - 1)).trim());
            System.out.println(HashCode.get(CommitNumber.get(i - 1)).trim());
            for (Map.Entry<String, Integer> entry : fc.entrySet()) {
                System.out.println("Name: " + entry.getKey() + ", CodeLines: " + entry.getValue());
            }
            System.out.println("*****************************************************");

            String[] CommitInfo = new String[100000];
            String CommitID = HashCode.get(CommitNumber.get(i)).trim();

            String OldCommitID = HashCode.get(CommitNumber.get(i - 1)).trim();
            FileRepository repo;
            RevWalk rw;
            try {
                repo = new FileRepository(new File(Gitpath));
                rw = new RevWalk(repo);
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
            RevCommit newcommit;
            RevCommit oldcommit;



            System.out.println(HashCode.get(CommitNumber.get(i)).trim());
//diff操作
            try {

                DiffEntry.Side sd = null;


                int linesAdded = 0;
                int linesDeleted = 0;
                int filesChanged = 0;

                /*********求FileName,AddLines,DeleteLines,CodeLines,FileAdd,FileDelete五个指标 ********/

                newcommit = rw.parseCommit(repo.resolve(HashCode.get(CommitNumber.get(i)).trim()));
                oldcommit = rw.parseCommit(repo.resolve(HashCode.get(CommitNumber.get(i - 1)).trim()));
                //System.out.println(oldcommit);
                DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
                df.setRepository(repo);
                df.setDiffComparator(RawTextComparator.DEFAULT);
                df.setDetectRenames(true);

                List<DiffEntry> diffs2;
                diffs2 = df.scan(oldcommit.getTree(), newcommit.getTree());
                //filesChanged = diffs2.size();

                LinkedHashMap<String, FileInfo> SFinfo = new LinkedHashMap<>();//存放单个文件名以及代码增减行数信息
                for (DiffEntry diff : diffs2) {
                    FileInfo FI = new FileInfo();
                    if(!diff.getOldPath().endsWith(".java") && !diff.getNewPath().endsWith(".java"))
                        continue;
                    filesChanged++;
                    for (Edit edit : df.toFileHeader(diff).toEditList()) {
                        linesAdded += edit.getEndB() - edit.getBeginB();
                        linesDeleted += edit.getEndA() - edit.getBeginA();
                        //System.out.println(edit);
                        FI.addNum += edit.getEndB() - edit.getBeginB();
                        FI.deleteNum += edit.getEndA() - edit.getBeginA();
                    }
                    //System.out.println(FI.addNum - FI.deleteNum);
                    String FileName = diff.getPath(sd);//删除的文件名变为dev/null
                    if (diff.getOldPath() != diff.getNewPath()) {
                        if (diff.getNewPath().contains("/dev/null")) {//删除文件
                            int m = diff.getOldPath().indexOf(".java");
                            FileName = diff.getOldPath();
                            FI.codelines = fc.get(FileName);
                            //fc.remove(diff.getOldPath().trim());

                        } else if (diff.getOldPath().contains("/dev/null")) {//增加文件
                            fc.put(diff.getNewPath(), 0);
                            FI.codelines = fc.get(FileName);
                            //fc.put(diff.getNewPath().trim(), FI.addNum - FI.deleteNum);
                        } else {//文件改名
                            //因为系统问题没有的文件 我们直接将其代码行数记成0
                            int oldlines = 0;
                            if(!fc.containsKey(diff.getOldPath())){
                                fc.put(diff.getOldPath(), 0);
                                oldlines = 0;
                            }
                            else
                                oldlines = fc.get(diff.getOldPath());
                            //fc.remove(diff.getOldPath());
                            fc.put(diff.getNewPath(), oldlines);
                            FI.codelines = fc.get(FileName);
                            //fc.put(diff.getNewPath(), oldlines + FI.deleteNum - FI.deleteNum);
                        }
                    }
                    else{//没有改名
                        //因为系统问题找不到的文件 代码行数设为0
                        if(!fc.containsKey(FileName))
                            FI.codelines = 0;
                        else
                            FI.codelines = fc.get(FileName);
                        //int lines = fc.get(diff.getPath(sd));
                        //fc.put(diff.getPath(sd), lines + FI.addNum - FI.deleteNum);
                    }
                    System.out.println(FileName);
                    SFinfo.put(FileName, FI);//存放本次提交的所有修改文件名
                }

                /**********输出剩余五个指标************/
                //增减文件数
                int FileAdd = 0;
                int FileDelete = 0;
                //int TotalCodeLines = 0;
                int LC = 0;
                LC = Math.abs(linesAdded - linesDeleted);
                CommitInfo[0] = CommitID + "," + filesChanged + "," + linesAdded + "," + linesDeleted + "," + LC;
                int n = 0;
                FileRepositoryBuilder builder = new FileRepositoryBuilder();
                Repository repo1 = builder.setGitDir(new File(Gitpath)).setMustExist(true).build();
                int NUC = 0;
                double AGE = 0.0; //单位是天
                int NS = 0;
                int ND = 0;
                double Entropy = 0.0;
                int LT = 0;
                int NDEV = 0;

                ArrayList<String> Subsystem = new ArrayList<>();
                ArrayList<String> Directory = new ArrayList<>();
                //每个文件的具体信息
                for (String s : SFinfo.keySet()) {
                    //System.out.println("Filename:" + s);
//                    int codelines;
//                    codelines = fc.get(s);
                    /******计算NUC、AGE、NS、ND、Entropy、LT*****/
                    /***均为提交级别指标 不是文件级别***/

                    //ArrayList<RevCommit> commits =  new  LogFollowCommand(repo, CommitID, s).call();

                    Git git = new Git(repo);
                    git.checkout().setName(CommitID).call();
                    int CS = 0;
                    Iterable<RevCommit> commits  = git.log().addPath(s).call();
                    List<RevCommit> result = new ArrayList<RevCommit>();
                    commits.forEach(result::add);
                    /************NDEV**********/

                    Set<String> Authors = new HashSet<>();
//                    for(int j = 0; j < commits.size(); j++){
//                        String Author =  commits.get(j).getAuthorIdent().getName();
//                        Authors.add(Author);
//                    }
                    for(RevCommit commit : result){
                        String Author =  commit.getAuthorIdent().getName();
                        Authors.add(Author);
                    }
                    NDEV = Authors.size();

                    CS = result.size();
                    if(CS > NUC)
                        NUC = CS;
//                    if(commits.size() > NUC)
//                        NUC = commits.size();


                    if(CS > 1) {
                        int day = ((result.get(0).getCommitTime() - result.get(1).getCommitTime()) / (60 * 20 * 24));
                        day = day / filesChanged;
                        AGE += day;
                    }
                    System.out.println(s);
                    String sub = "";
                    int nsend = 0;
                    int ndend = 0;
                    if(s.indexOf('/') != -1) {
                        nsend = s.indexOf('/');
                        sub = s.substring(0, nsend - 1);
                        ndend = s.lastIndexOf('/');
                    }
                    else
                        sub = "null";
                    if(!Subsystem.contains(sub))
                        Subsystem.add(sub);


                    String Dir = "";
                    if(nsend != ndend)
                        Dir = s.substring(nsend + 1, ndend - 1);
                    else
                        Dir = sub;
                    if(!Directory.contains(Dir))
                        Directory.add(Dir);

                    double Filechanged =  SFinfo.get(s).addNum + SFinfo.get(s).deleteNum;
                    double CommitChanged = linesAdded + linesDeleted;
                    //double en =  (double)(Filechanged/CommitChanged) * (Math.log(Filechanged/CommitChanged) / Math.log(2));
                    if(Filechanged != 0)
                        Entropy -=  (Filechanged/CommitChanged) * (Math.log((Filechanged/CommitChanged)) / Math.log(2));
//                    System.out.println(CommitChanged);
//                    System.out.println(Math.log(Filechanged/CommitChanged) / Math.log(2));
//                    BigDecimal entr = new BigDecimal(en);
//                    Entropy -= entr.setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();

                    LT += SFinfo.get(s).codelines;


                    if (s != null) {
                        if (CommitInfo[n] != null)
                            CommitInfo[n] = CommitInfo[n] + "," + s + "," + SFinfo.get(s).addNum + "," + SFinfo.get(s).deleteNum + "," +SFinfo.get(s).codelines;
                        else
                            CommitInfo[n] = ",,,,," + s + "," + SFinfo.get(s).addNum + "," + SFinfo.get(s).deleteNum + "," + SFinfo.get(s).codelines;
                    }
                    //增减文件数计算
                    if((SFinfo.get(s).deleteNum == 0) && (SFinfo.get(s).deleteNum == SFinfo.get(s).codelines))
                        FileAdd++;
                    if((SFinfo.get(s).addNum == 0) && (SFinfo.get(s).deleteNum == SFinfo.get(s).codelines) && (SFinfo.get(s).deleteNum != 0))
                        FileDelete++;
                    //TotalCodeLines += SFinfo.get(s).codelines;
                    n++;
                }


                NS = Subsystem.size();
                ND = Directory.size();

                /**************获取作者经验EXP,REXP******************/
                int EXP = 0;
                int REXP = 0;
                Git git = getGitObj(repo);//获得git对象
                git.checkout().setName(CommitID).call();
                Iterable<RevCommit> log = git.log().call();

                String AuthorName = "";
                int CommitTime = 0;
                int ci = 0;

                for (RevCommit commit : log) {
                    if(ci == 0) {
                        AuthorName = commit.getAuthorIdent().getName();
                        CommitTime = commit.getCommitTime();
                    }
                    //System.out.println(commit.getId());
                    //System.out.println(commit.getAuthorIdent().getName());
                    if( ci != 0 && AuthorName.equals(commit.getAuthorIdent().getName())) {
                        EXP++;
                        if(CommitTime - commit.getCommitTime() <= 1200000)
                            REXP++;
                    }

                    ci++;

                }
                /*********************/

                //本提交中没有修改文件的情况
                if(n == 0){
                    CommitInfo[n] = CommitInfo[n] + ",/," + 0 + "," + 0 + "," + 0;
                }
                int NAD = FileAdd + FileDelete;
                CommitInfo[0] = CommitInfo[0] + "," + FileAdd + "," + FileDelete + "," + NAD + "," + NDEV + "," + NUC + "," + AGE + "," + NS + "," + ND + "," + Entropy + "," + LT + "," + EXP + "," + REXP;
                //CommitInfo[0] = CommitInfo[0] + "," + FileAdd + "," + FileDelete + "," + NDEV + "," + NUC + "," + AGE + "," + NS + "," + ND + "," + Entropy + "," + LT;
                for (int m = 0; CommitInfo[m] != null; m++) {
                    bw1.write(CommitInfo[m]);
                    bw1.newLine();
                    bw1.flush();
                }
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            } catch (GitAPIException e) {

            }

            System.out.println("**************************************");

        }

        bw1.close();
        //bw2.close();

        /********文件增减行数表格的表头为 CommitID,NMF,TotalAddLines,TotalDeleteLines,FileName,AddLines,DeleteLines,CodeLines,FileAdd,FileDelete,NDEV,NUC,AGE,NS,ND,Entropy,TLMF，EXP,REXP *****/

    }

    public static void GetHashCode(HashMap<Integer, String> H, List<Integer> CommitNumber, String Mappath) {
        //H里记录的是所有的CommitID CommitNumber里记录的是除第一个CommitID外的所有ID 因为第一个提交和第二个提交之间的源代码差异是第二个提交造成的，因此第一个提交不做研究
        try {
            BufferedReader f = new BufferedReader(new FileReader(Mappath));//s为log地址
            String str;
            int i = 0;
            while ((str = f.readLine()) != null)//逐行读文件
            {

                //str = str.trim();//去掉空格

                String[] str2;

                str2 = str.split(" ");
                if (str2.length < 2)//最后一行不要
                    continue;

                str2[2] = str2[2].trim();
                int index = str2[2].indexOf("Value");
                str2[2] = str2[2].substring(0, index - 1);
                H.put(Integer.valueOf(str2[4]), str2[2]);

                CommitNumber.add(Integer.valueOf(str2[4]));
                // System.out.println(str2[2] + " " + str2[4]);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static Repository getRepository(String dir) {
        try {
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(new File(dir + "/.git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();
            return repository;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    //获取GIT对象
    public static Git getGitObj(Repository repository) {
        Git git = null;
        git = new Git(repository);
        return git;
    }

    public static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(ObjectId.fromString(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }
            walk.dispose();
            return treeParser;
        }
    }



}

class FileInfo {
    int addNum = 0;
    int deleteNum = 0;
    int codelines = 0;

    FileInfo() {
    }
}

class LogFollowCommand {

    private final Repository repository;
    private String path;
    private Git git;

    private String CommitID;

    /**
     * Create a Log command that enables the follow option: git log --follow -- < path >
     * @param repository
     * @param path
     */
    public LogFollowCommand(Repository repository,String CommitID, String path){
        this.repository = repository;
        this.path = path;
        this.CommitID = CommitID;
    }

    /**
     * Returns the result of a git log --follow -- < path >
     * @return
     * @throws IOException
     * @throws MissingObjectException
     * @throws GitAPIException
     */
    public ArrayList<RevCommit> call() throws IOException, MissingObjectException, GitAPIException {
        ArrayList<RevCommit> commits = new ArrayList<RevCommit>();
        git = new Git(repository);
        git.checkout().setName(CommitID).call();
        RevCommit start = null;
        do {
            Iterable<RevCommit> log = git.log().addPath(path).call();
            for (RevCommit commit : log) {
                if (commits.contains(commit)) {
                    start = null;
                } else {
                    start = commit;
                    commits.add(commit);
                }
            }
            if (start == null) return commits;
        }
        while ((path = getRenamedPath( start)) != null);

        return commits;
    }

    /**
     * Checks for renames in history of a certain file. Returns null, if no rename was found.
     * Can take some seconds, especially if nothing is found... Here might be some tweaking necessary or the LogFollowCommand must be run in a thread.
     * @param start
     * @return String or null
     * @throws IOException
     * @throws MissingObjectException
     * @throws GitAPIException
     */
    private String getRenamedPath( RevCommit start) throws IOException, MissingObjectException, GitAPIException {
        Iterable<RevCommit> allCommitsLater = git.log().add(start).call();
        for (RevCommit commit : allCommitsLater) {

            TreeWalk tw = new TreeWalk(repository);
            tw.addTree(commit.getTree());
            tw.addTree(start.getTree());
            tw.setRecursive(true);
            RenameDetector rd = new RenameDetector(repository);
            rd.addAll(DiffEntry.scan(tw));
            List<DiffEntry> files = rd.compute();
            for (DiffEntry diffEntry : files) {
                if ((diffEntry.getChangeType() == DiffEntry.ChangeType.RENAME || diffEntry.getChangeType() == DiffEntry.ChangeType.COPY) && diffEntry.getNewPath().contains(path)) {
                    System.out.println("Found: " + diffEntry.toString() + " return " + diffEntry.getOldPath());
                    return diffEntry.getOldPath();
                }
            }
        }
        return null;
    }
}
