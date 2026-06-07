# 14_文件与IO 练习题

共 1 道题目

## Q047` 要从文件"file.dat"中读出第10个字节到变量c中，下列哪个方法适合？
- A. `FileInputStream in=new FileInputStream("file.dat"); in.skip(9); int c=in.read();`
- B. `FileInputStream in=new FileInputStream("file.dat"); in.skip(10); int c=in.read();`
- C. `FileInputStream in=new FileInputStream("file.dat"); int c=in.read();`
- D. `RandomAccessFile in=new RandomAccessFile("file.dat"); in.skip(9); int c=in.readByte();`



`

---

