// 递归 Java 实操案例
// 说明：这是示例代码，供练习参考。

public class Main {
    public static void main(String[] args) {
        // 示例：创建对象并调用方法
        Student s = new Student("张三", 20);
        System.out.println(s.getName());
    }
}

class Student {
    private String name;
    private int age;

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
}