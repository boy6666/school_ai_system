# Java Swing 教程

## Java Swing 介绍

### 分类编程技术

ad start
ad end
Swing 是一个为Java设计的GUI工具包。

Swing是JAVA基础类的一部分。

Swing包括了图形用户界面（GUI）器件如：文本框，按钮，分隔窗格和表。

Swing提供许多比AWT更好的屏幕显示元素。它们用纯Java写成，所以同Java本身一样可以跨平台运行，这一点不像AWT。它们是JFC的一部分。它们支持可更换的面板和主题（各种操作系统默认的特有主题），然而不是真的使用原生平台提供的设备，而是仅仅在表面上模仿它们。这意味着你可以在任意平台上使用JAVA支持的任意面板。轻量级组件的缺点则是执行速度较慢，优点就是可以在所有平台上采用统一的行为。

## Hello World 程序

HelloWorldSwing.java 文件代码如下：

```
import javax.swing.*;
public class HelloWorldSwing {
    /**{
     * 创建并显示GUI。出于线程安全的考虑，
     * 这个方法在事件调用线程中调用。
     */
    private static void createAndShowGUI() {
        // 确保一个漂亮的外观风格
        JFrame.setDefaultLookAndFeelDecorated(true);

        // 创建及设置窗口
        JFrame frame = new JFrame("HelloWorldSwing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 添加 "Hello World" 标签
        JLabel label = new JLabel("Hello World");
        frame.getContentPane().add(label);

        // 显示窗口
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        // 显示应用 GUI
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
```

执行以下命令输出结果：

```
$ javac HelloWorldSwing.java
$ java HelloWorldSwing
```

### 一个用户登录框实例

SwingLoginExample.java 文件代码如下：

```
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField; 
public class SwingLoginExample {
    
    public static void main(String[] args) {    
        // 创建 JFrame 实例
        JFrame frame = new JFrame("Login Example");
        // Setting the width and height of frame
        frame.setSize(350, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        /* 创建面板，这个类似于 HTML 的 div 标签
         * 我们可以创建多个面板并在 JFrame 中指定位置
         * 面板中我们可以添加文本字段，按钮及其他组件。
         */
        JPanel panel = new JPanel();    
        // 添加面板
        frame.add(panel);
        /* 
         * 调用用户定义的方法并添加组件到面板
         */
        placeComponents(panel);

        // 设置界面可见
        frame.setVisible(true);
    }

    private static void placeComponents(JPanel panel) {

        /* 布局部分我们这边不多做介绍
         * 这边设置布局为 null
         */
        panel.setLayout(null);

        // 创建 JLabel
        JLabel userLabel = new JLabel("User:");
        /* 这个方法定义了组件的位置。
         * setBounds(x, y, width, height)
         * x 和 y 指定左上角的新位置，由 width 和 height 指定新的大小。
         */
        userLabel.setBounds(10,20,80,25);
        panel.add(userLabel);

        /* 
         * 创建文本域用于用户输入
         */
        JTextField userText = new JTextField(20);
        userText.setBounds(100,20,165,25);
        panel.add(userText);

        // 输入密码的文本域
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(10,50,80,25);
        panel.add(passwordLabel);

        /* 
         *这个类似用于输入的文本域
         * 但是输入的信息会以点号代替，用于包含密码的安全性
         */
        JPasswordField passwordText = new JPasswordField(20);
        passwordText.setBounds(100,50,165,25);
        panel.add(passwordText);

        // 创建登录按钮
        JButton loginButton = new JButton("login");
        loginButton.setBounds(10, 80, 80, 25);
        panel.add(loginButton);
    }

}
```

执行以下命令输出结果：

```
$ javac SwingLoginExample.java
$ java SwingLoginExample
```

### 概念解析：

JFrame– java的GUI程序的基本思路是以JFrame为基础，它是屏幕上window的对象，能够最大化、最小化、关闭。JPanel–  Java图形用户界面(GUI)工具包swing中的面板容器类，包含在javax.swing 包中，可以进行嵌套，功能是对窗体中具有相同逻辑功能的组件进行组合，是一种轻量级容器，可以加入到JFrame窗体中。。

JLabel– JLabel 对象可以显示文本、图像或同时显示二者。可以通过设置垂直和水平对齐方式，指定标签显示区中标签内容在何处对齐。默认情况下，标签在其显示区内垂直居中对齐。默认情况下，只显示文本的标签是开始边对齐；而只显示图像的标签则水平居中对齐。

JTextField–一个轻量级组件，它允许编辑单行文本。

JPasswordField– 允许我们输入了一行字像输入框，但隐藏星号(*) 或点创建密码(密码)

JButton– JButton 类的实例。用于创建按钮类似实例中的 "Login"。

←
js 定时器
HBuilder 使用教程
→
### 点我分享笔记

<p style="font-size:14px;">笔记需要是本篇文章的内容扩展！</p><br>
	<p style="font-size:12px;"><a href="//www.runoob.com/tougao" target="_blank">文章投稿，可点击这里</a></p>
	<p style="font-size:14px;"><a href="/w3cnote/runoob-user-test-intro.html#invite" target="_blank">注册邀请码获取方式</a></p>
		<h3 class="text-muted"><i class="fa fa-info-circle" aria-hidden="true"></i> 分享笔记前必须<a href="javascript:;" class="runoob-pop">登录</a>！</h3>
		<p><a href="/w3cnote/runoob-user-test-intro.html#invite" target="_blank">注册邀请码获取方式</a></p>
分享笔记
- 昵称昵称 (必填)
- 邮箱邮箱 (必填)
- 引用地址引用地址

