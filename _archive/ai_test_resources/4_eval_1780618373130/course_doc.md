你好，我是你的课程讲解专家。今天我们要一起来解决一个让你反复卡住、迟迟无法动手的核心问题：如何正确安装JDK并配置环境变量，然后真正运行起你的第一个Java程序。很多同学和你的情况很像，总是想先拿到完美的学习规划，却忘记了编程最关键的步骤是“把代码敲出来并看到结果”。下面我会用最直白的话解释概念，带你亲手写出两个可以运行的代码示例，并指出你最容易踩坑的几个地方。

首先，我们需要搞清楚几个核心概念。

JDK的全称是Java Development Kit，也就是Java开发工具包。它包含了三个重要的东西：编译器（javac）、解释器（java）和JRE（Java运行时环境）。你安装JDK后，会在某个文件夹里看到bin目录，这个bin目录里放着javac.exe和java.exe等可执行文件。在命令行里输入javac，就是调用这个编译器把你写的.java文件编译成.class字节码文件；然后输入java，就是调用解释器去运行这个.class文件。PATH环境变量就像是操作系统的“通讯录”，你告诉系统说：当我在命令行里输入javac或java时，你要去哪些文件夹里找这些程序。如果你没有把JDK的bin目录加到PATH里，系统就会提示“javac不是内部或外部命令”。所以配置环境变量就是让系统知道你的Java工具放在哪里。

现在我们来动手。我假设你已经从Oracle官网下载了JDK（比如JDK 17），并安装到了C:\Program Files\Java\jdk-17（注意，路径里不要有中文和空格，如果安装时有自定义选项，建议改成一个简单路径比如C:\Java\jdk-17）。安装完成后，你需要做两件事：一是设置JAVA_HOME环境变量，指向你的JDK安装目录；二是把%JAVA_HOME%\bin加到PATH里。具体操作：在Windows搜索“环境变量”，点击“环境变量”，在系统变量里新建一个JAVA_HOME，值填C:\Java\jdk-17；然后找到PATH变量，编辑，在末尾添加一行%JAVA_HOME%\bin（注意前面如果有分号则无需再加分号，Win10/11可以一行一行添加）。之后打开新的命令提示符窗口，输入java -version，如果显示版本信息，就说明配置成功。

下面给你两个可以运行的代码示例，请你一定亲自在记事本或VS Code里写一遍，用命令行编译运行。

第一个示例：HelloWorld（基础中的基础）。新建一个文本文档，改名为HelloWorld.java（注意大小写）。内容如下：

public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, 这是我的第一个Java程序！");
    }
}

然后在命令提示符中，cd到该文件所在的文件夹，依次输入：
javac HelloWorld.java
java HelloWorld
如果屏幕上打印出了那句话，你就成功了。注意：javac命令后要跟文件名（带.java），java命令后要跟类名（不带.class，也不带后缀）。

第二个示例：做一个简单的加法计算器，练习基本的输入输出和变量使用。新建一个Calculator.java文件，内容如下：

import java.util.Scanner;

public class Calculator {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入第一个数字: ");
        int a = scanner.nextInt();
        System.out.print("请输入第二个数字: ");
        int b = scanner.nextInt();
        int sum = a + b;
        System.out.println("两数之和是: " + sum);
        scanner.close();
    }
}

同样用javac编译，用java运行，然后根据提示输入两个整数，就能看到结果。

现在列出你在配置和运行中常犯的五个易错点，请你对照检查：

第一，配置PATH时，很多同学会漏掉bin目录，或者把路径写成了JDK安装目录本身。正确的写法是C:\Java\jdk-17\bin，而不是C:\Java\jdk-17。

第二，类名必须和文件名完全一致。如果你写的类是public class HelloWorld，那么文件名必须是HelloWorld.java，大小写也不能错。

第三，main方法的签名必须一字不差：public static void main(String[] args)。如果你写成public static void main(String args[]) 也是可以的，但初学者经常漏掉static或者把单词拼错。

第四，编译时遇到“javac不是内部命令”，90%的情况是PATH没配好。请确认你打开的是新命令提示符窗口（旧窗口不会刷新环境变量），并且输入echo %JAVA_HOME%和echo %PATH%来检查变量是否正确。

第五，运行java时，不要在类名后加.class或.java。比如java HelloWorld是正确的，java HelloWorld.class是错的。

最后给你几条学习建议，请你认真执行，不要再停留在“请求规划”阶段。

第一，立刻停止向AI索要新的学习路线图。你在过去已经收到了足够多的指导，现在你需要的是在电脑前坐下来，输入上面两个代码，亲眼看到输出结果。十五分钟之内就能完成。

第二，建立一个习惯：每天至少写一个简单的小程序，哪怕只是打印一句“今天天气不错”。动手之后你会发现自己真正的问题在哪里，而不是空想。

第三，遇到报错不要直接问AI“为什么”，而是先自己读一下错误信息。比如“找不到符号”说明变量名写错了，“非法字符”说明可能用了中文标点。试着解读，然后再去搜索或用AI确认。

第四，环境变量配好之后，不要再反复重装JDK或改动配置。你已经成功了，把时间用在写代码上。

记住，编程不是靠看规划学会的，是靠敲击键盘学会的。现在就去打开编辑器，写下你的HelloWorld，然后让它跑起来。