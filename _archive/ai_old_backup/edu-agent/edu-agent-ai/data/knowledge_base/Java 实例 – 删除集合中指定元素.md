# Java 实例 – 删除集合中指定元素

**题目描述**: 以下实例演示了如何使用 Collection 类的 collection.remove() 方法来删除集合中的指定的元素：

```java

import java.util.*;
 
public class Main {
public static void main(String [] args) {
System.out.println( "集合实例!\n" ); 
      int size;
      HashSet collection = new HashSet ();
      String str1 = "Yellow", str2 = "White", str3 = 
      "Green", str4 = "Blue";  
      Iterator iterator;
      collection.add(str1);    
      collection.add(str2);   
      collection.add(str3);   
      collection.add(str4);
      System.out.print("集合数据: ");  
      iterator = collection.iterator();     
      while (iterator.hasNext()){
System.out.print(iterator.next() + " ");  
      }
System.out.println();
      collection.remove(str2);
      System.out.println("删除之后 [" + str2 + "]\n");
      System.out.print("现在集合的数据是: ");
      iterator = collection.iterator();     
      while (iterator.hasNext()){
System.out.print(iterator.next() + " ");  
      }
System.out.println();
      size = collection.size();
      System.out.println("集合大小: " + size + "\n");
   }
}

```

[原文链接](https://www.runoob.com/java/collection-remove.html)
