Java虚拟机的运行时数据区
==============

书籍参考：《深入理解Java虚拟机》


前言
--
之前对于jvm的数据区，大概只能说出一些来，但是对其中的各个区的作用及存储内容都不甚了解，前几天参见几篇博客，仔细的读了一遍，觉得豁然开朗，故记录下来避免以后忘了。之前也看过jvm的书，可是看完就忘了，最近打算再仔细读一遍，毕竟读书这种东西，每一遍都会有不同的感悟，不可能一遍就吃透。
![Java虚拟机运行时内存](http://on-img.com/chart_image/59d774bee4b017b41979c0eb.png)

这就是jvm运行时数据区的示意图，可以看到，整个运行区可以按照线程共享和线程隔离分为两类：

 - 线程共享区：
顾名思义，线程共享区是整个jvm数据共享的区域，不管是那个线程，都可以共享属于共享区的数据，例如存储在堆内存中的对象，存储在方法区中的已经被类加载器加载好的类信息等。

 - 线程独享区：
这部分数据是只有本线程能够单独持有的，例如局部变量，该线程中程序计数器指向的当前线程运行的行号等。

我们经常说的线程安全，其本质就是为了防止线程共享区的数据被线程其它线程篡改，从而引发线程安全问题，而这种安全性问题在线程独享的数据区域则不会存在。

程序计数器
-----
从上面的图可以看出，程序计数器是线程独享的，用来记录当前程序执行的指令 ，也可以看作程序当前执行的行号。jvm中的程序计数器只记录java方法执行的字节码指令，不会记录native层方法的指令。其实也很好理解，jvm就是用来执行字节码指令的虚拟机，和native层的通信交流大部分都在外部处理好了。

虚拟机栈
----
之前一直对这一块搞不太清楚，因为之前的思路总是在区分栈和堆，对其中具体内容反而没那么关注。顾名思义，虚拟机栈是一个栈结构，是线程独享的，那么作为一种栈的数据结构，它存储的是什么内容呢？这里我们要引入一个概念------栈帧。

栈帧
--

那么栈帧又是什么呢？每伴随着一个方法被调用，jvm就会创建一个栈帧，并且压入虚拟机栈，伴随着方法调用结束，该栈帧就会出栈，并且被销毁。也就是说一个栈帧的声明周期就是它对应的方法的执行周期。一个线程的调用方法链可能很长，很多方法都同时出于执行状态，因此在活动的线程中，只有位于栈顶的栈帧才是有效的。

![虚拟机栈图](https://user-gold-cdn.xitu.io/2017/9/22/dc9ec811c1969025c147bbfc3fd845dc?imageView2/0/w/1280/h/960)
我们知道栈帧的生命周期就是对应的java方法执行的周期，所以栈帧需要存储方法执行时的一些数据。栈帧中存放的数据包括：局部变量表，操作数栈，动态连接和返回地址等。

 局部变量表
-------

局部变量表是一组变量值的存储空间，用于存放方法的参数和方法内部定义的局部变量。在java程序编译为class文件时，就在该方法的max——locals数据项中确定了该方法所需要分配的局部变量的最大容量。

局部变量表以变量槽为最小单位(Slot),存放了编译时期的各种基本数据类型(boolean  byte  char  short  int  float  long  double)，reference(指向对象的引用或者代表对象的句柄等)和returnAddress(指向了一条字节码指令的地址)。其中64位的long和double会占用2个局部变量空间，其余的数据类型只占一个。

在方法执行时，虚拟机使用局部变量表完成参数值到参数变量列表的传递过程的。为了尽可能实现节省栈帧控件，局部变量表中的slot是可以复用的，方法体中的变量，其作用域并不一定会覆盖整个方法体，那么这个变量对应的slot就可以交给其它变量来使用。不过这样的设计除了节省内存，在部分情况下还会直接影响到垃圾收集的行为：

```
public static void main(String[] args) {
        byte[] bytes = new byte[64 * 1024 * 1024];
        System.gc();
    }
```
	

```
public static void main(String[] args) {
        {
            byte[] bytes = new byte[64 * 1024 * 1024];
        }
        System.gc();
    }
```
像上面这两种情况，没有任何对局部变量表的操作，由于slot的复用，也就是说当没有其它变量来占用bytes所占用的slot时，slot仍然持有bytes的引用，这种关联没有被打断，绝大多数情况都是没多大问题的，但是如果一个方法其后面是一段耗时很长的操作，而前面又定义了占用大量内存但实际上不会再使用的变量，手动将其设置为null是很有必要的。
```
    public static void main(String[] args) {
        {
            byte[] bytes = new byte[64 * 1024 * 1024];
        }
        int a = 0;
        System.gc();
    }
```
像这种的就不gc时，bytes申请的64m的内存就可以被回收了，因为bytes占用的slot槽被变量a复用了。不过实际中由于很多编译器做了优化，我们也不需要刻意地去这样做。

还有一点重要的是局部变量是不会像类变量一样被加载的，系统不会默认为局部变量赋初始值，所以一个局部变量如果不赋值是不能使用的，像如下代码，编译器是会报错的。

```
public static void main(String[] args) {
        int a;
        System.out.println(a);
    }
```

操作数栈
----

操作数栈也常称为操作栈，同样是一个栈结构。同局部变量表一样，操作数栈的最大深度也在编译的时候就会写入到Class文件中Code属性的
max_stacks数据项中。

当一个方法刚开始执行的时候，操作数栈是空的，在方法执行的过程中，会有各种字节码在操作数栈上写入和提取内容，也就对应着入栈和出栈操作。如果对学习数据结构栈的应用有映像的话，当时我们做四则运算的和中缀转后缀表达式的时候，都是利用栈来进行运算的。所以操作数栈刚好用到这种方式。在算数运算和调用其它方法的时候，使用操作数栈进行运算和参数传递的。

概念模型里，栈帧之间是应该是相互独立的，不过大多数虚拟机都会做一些优化处理，使两个栈帧之间有部分重叠，这样在进行方法调用的时候可以直接共用参数，而不需要做额外的参数复制等工作。重叠过程如图所示：

![这里写图片描述](https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507311632073&di=2a164e59a16cb1007a37e29048b1ebe4&imgtype=0&src=http://static.oschina.net/uploads/space/2015/0410/083331_WI2x_245653.gif)


动态连接
----
每个栈帧都包含一个指向运行时常量池中该栈帧所属方法的引用，Class文件的常量池中存有大量的符号引用，字节码中的方法调用指令就以常量池中方法的符号引用为参数。这些符号引用一部分会在类加载阶段或者第一次使用的时候就转化为直接引用（静态方法，私有方法等），这种转化称为静态解析，另一部分将在每一次运行期间转化为直接引用，这部分称为动态连接。由于篇幅有限这里不再继续讨论解析与分派的过程，这里只需要知道静态解析与动态连接的区别就好。

方法返回地址
------
当一个方法开始执行后，只有两种方式可以退出这个方法:

 1. 执行引擎遇到任意一个方法返回的字节码指令:
	传递给上层的方法调用者，是否有返回值和返回值类型将根据遇到何种方法来返回指令决定，这种退出的方法称为正常完成出口。
 
 2. 方法执行过程中遇到异常：
	 无论是java虚拟机内部产生的异常还是代码中thtrow出的异常，只要在本方法的异常表中没有搜索到匹配的异常处理器，就会导致方法退出，这种退出的方式称为异常完成出口，一个方法若使用该方式退出，是不会给上层调用者任何返回值的。

无论使用那种方式退出方法，都要返回到方法被调用的位置，程序才能继续执行，方法返回时可能会在栈帧中保存一些信息，用来恢复上层方法的执行状态。一般方法正常退出的时候，调用者的pc计数器的值可以作为返回地址，帧栈中很有可能会保存这个计数器的值作为返回地址。

方法退出的过程就是栈帧在虚拟机栈上的出栈过程，因此退出时的操作可能有：恢复上层方法的局部变量表和操作数栈，把返回值压入调用者的操作数栈每条整pc计数器的值指向调用该方法的后一条指令。

案例
-----

```
    public static void main(String[] args) {
        int a = 3;
        int b = 4;
        int c = 5;
        int d = add(a, b) + add(a, c);
    }

    private static int add(int a, int b) {
        return a + b;
    }
```
按照我们之前的思路，开始对两个方法的栈帧及内部的数据开始进行分析。

首先，在main方法中，首先初始化局部变量表，依次给参数分配slot，然后在局部变量表中分配3个slot用来给变量a，b，c，d分配内存空间，然后两次调用add方法，传入不同的参数，伴随着两个add栈帧的创建，add栈帧内部局部变量表的初始化，add操作数栈对于参数的加法运算，运算完毕返回返回值给main方法，main方法先获得a+b的返回值，压入操作数栈，再获得a+c的返回值，压入操作数栈，最后碰到运算符"+"，两个操作数出栈进行运算，将运算结果再压入操作数栈，赋值给d（这里的计算机自动中缀转后缀）。

本地方法栈
-----
本地方法栈与虚拟机栈作用相似，最大的区别就是：虚拟机栈为虚拟机执行的java方法（字节码）服务，而本地方法为虚拟机使用到的native层方法提供服务。我们熟悉的JNI和NDK实际上就是在编写本地方法，被java方
法调用或者调用java方法。

本地方法栈也有一个专门的程序计数器来记录本地方法的执行情况。

Java堆内存
-------
对于大多数应用来说，Java堆内存是Java虚拟机管理内存中最大的一块。Java堆是线程共享区域，在虚拟机启动的时候就会被创建。这块内存的目的就是存放对象实例，几乎所有的对象都在这里分配内存。根据java虚拟机规范：所有的对象实例和数组都要在堆上分配内存。

Java堆是GC回收的主要区域，Java语言通过可达型分析来判定对象是否存活的。算法的基本思想是通过一系列称为"GC Roots"的对象作为跟节点，从这些节点往下搜索，搜索走过的路径称为引用链，当一个对象到GC Roots没有任何引用链连接的话，则证明该对象不可用，将被判定为可回收的对象。

java堆内存可以细分为新生代、老年代和永久代。现在的垃圾收集器基本都基于分代收集算法，因此这样细分java堆内存空间也是很有必要的。同样的，作为线程共享区的Java堆也可以分出多个线程私有的分配缓冲区(Thread Local Allocation Buffer)，不管如何划分，都是为了更好的存储对象，合理的利用空间进行内存的分配与回收。
![这里写图片描述](https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507395389409&di=e4b760f1f52b427d52c7c369c64501d8&imgtype=0&src=http://image.codes51.com/Article/image/20160617/20160617153813_3945.jpg)

新生代
---
新生代分为：Eden区和两块Survivor区（From Survivor区和To Survivor区）。这三个区的内存占比一般为8:1:1

对象的分配一般是主要是在新生代的Eden区的，我们平时写代码的时候通过 "new"关键字或者反射、动态代理等等，总之就是创建新对象的方式，这种方式创建一个新的对象大部分情况下都是在Eden区分配内存的。如果启动了本地线程分配缓冲，将按照线程优先级在TLAB（线程私有分配缓冲区）上分配，少数情况下可能直接分配在老年代中。分配的规则取决于垃圾收集器和内存参数配置等。

当Eden没有足够空间分配的时候，虚拟机会触发一次GC，虚拟机GC完成后会将Eden区和已经分配的Survivor区中还存活的对象一次性复制另一个 Survivor区，如果这块Survivor空间不够用，则会在老年代中进行分配担保。

老年代
---
在新生代的Eden区不够分配且gc后剩余的Survivor区也不够分配时，会在老年代触发分配担保进行内存分配。除了这种情况，如果对象在Eden区分配的内存，并经过第一次GC后移动到Survivor空间，又经过多次GC后仍然在Survivor区中没有被回收（默认15次），就会被晋升到老年代中。

值得注意的是，并不一定非要达到系统默认GC次数对象才会被晋升到老年代，如果Survivor空间中相同年龄对象大小总和大于Survivor空间的一半，年龄大于或等于该年龄的对象就会直接进入老年代，不需要达到最大GC次数。

永久代
---
关于永久代，其实只针对Java8之前的HotSpot虚拟机，在别的虚拟机上没有这个概念。这里的永久代实际上是方法区在HotSpot虚拟机上的内存实现位置。而在不同的虚拟机中，方法区的实现可以放在不同的位置。在Java8中HotSpot也废除了永久区，方法区存放在一个与堆不相连的本地区域------元空间。关于元空间的内容，详情请参见[Java永久代去哪儿了](http://blog.csdn.net/chenleixing/article/details/48286127)。

方法区
---
方法区与Java堆一样，是线程共享的区域，它用于存储已经被虚拟机加载的类信息、常量、静态变量、即时编译器编译后的代码等数据。Java虚拟机规范把方法区放在堆中作为堆的物理部分，但是伴随着最新的Java8到来HotSpot虚拟机也把方法去放在了堆外的本地内存空间，而且方法区和堆，这两者本身逻辑上并没有联系。

方法区的内存也需要回收，但是一般这个区域的内存回收比较难，因为之前对类信息进行装载完毕后才能进入方法区，但是后面对于类型的卸载和常量池的回收，条件相当的苛刻。

运行时常量池
------
运行时常量池是方法区的一部分。Class文件中除了有类的版本、字段、方法、接口等描述信息，还有一项信息是常量池，用于存放编译期间生成的各种字面量和符号引用，这部分内容将在类加载之后进入方法区的运行时常量池中存放。

一般来说，运行时常量池除了保存Class文件中描述的符号引用之外，还会把翻译出来的直接引用也存储在运行时常量池中。

直接内存
----
直接内存并不属于java虚拟机的运行时数据区，但是这部分内存会被频繁地使用。在JDK1.4中新加入了NIO，是基于通道与缓冲区的IO方式，可以使用native函数直接在堆外分配内存，通过存储在java堆中的DirectByteBuffer对象作为这块内存的引用来进行操作，这样可以显著的提升性能，避免在java堆和native堆中来回复制数据对机器性能的损耗。

这是一种堆外分配很好的方式，但是既然是内存，还是会受到机器的总内存大小和处理器寻址空间的限制。



