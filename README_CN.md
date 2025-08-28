## Woody
Woody项目是一个专注于Java应用性能问题分析诊断的工具，目前在不断完善功能中。

目前碰到以下情形可以通过Woody来诊断分析:

  1. 应用GC频率较高，需要定位哪些业务请求，哪些代码分配内存较多，以及分配次数和分配字节数
  2. 应用CPU使用率较高，需要定位是哪些业务请求，哪些代码使用了大量CPU资源，以及cpu资源消耗比例
  3. 应用接口耗时较长，定位接口内部的哪些操作，哪些代码消耗时间，以及相应的时间消耗比例
  4. 应用锁竞争激烈，需要精准优化
  5. 某个业务接口，某个特定请求有性能问题(cpu,内存，耗时),需要精确定位问题

Woody目前只支持jdk1.8+，支持mac，linux x64/arm64，低版本的jdk和其他操作系统暂时不支持。通过命令行交互，配合async-profiler, 输出采样样本和火焰图来分析定位问题。火焰图查看方式请自行AI学习。

Woody能将业务请求和火焰图样本精确关联，可手动过滤不需要分析的业务入口，提高采样精准率，能将消耗降的很低。

工程少量代码借鉴自arthas, 主要是agent,spy模块,不重复造轮子。

### 支持中间件列表:
  1. SpringMVC
  2. Dubbo
  3. Grpc
  4. Kafka
  5. RocketMQ

目前只支持上述5个中间件，后续会不断丰富。

### 快速开始

从项目的release界面下载最新`woody-boot-xxx.jar`版本，然后用`java -jar` 形式启动
```bash
  java -jar woody-boot-1.0.0.jar
```
<img width="1000" height="406" alt="image" src="https://github.com/user-attachments/assets/3f065671-762e-4b30-a5f5-1e070ee03715" />

然后选择对应的java进程编号，之后会进入命令交互界面。

### 命令列表

#### pr(profiling resource)
此命令的作用是选择要分析诊断的业务入口，可以同时选多种中间件的多个业务入口。

参数列表:
* -ls: list resource, 列举出当前应用的业务入口资源
<img width="400" height="400" alt="image" src="https://github.com/user-attachments/assets/3dad9c98-ba42-4551-8208-0dd96730915c" />

* -lt: list resource type, 列举出当前应用的所有业务资源类型
<img width="400" height="132" alt="image" src="https://github.com/user-attachments/assets/cf12ca39-750b-46df-8f0f-51ee450ca013" />


* -s : select, 选择业务入口资源
<img width="600" height="130" alt="image" src="https://github.com/user-attachments/assets/cf876f61-32e7-443f-9c6a-4469be4b0aa6" />

* -us: unselect, 移除已选中的业务入口资源
<img width="460" height="318" alt="image" src="https://github.com/user-attachments/assets/b16e1ef0-1b7e-40d5-a739-51ec3acf2c8c" />


* -lst: list selected resource types, 列举出已选择业务入口资源类型列表，没选时为[]
<img width="400" height="138" alt="image" src="https://github.com/user-attachments/assets/2982c61e-7882-440a-906b-bc8020589450" />


* -lss: list selected resource, 列举出已选择的业务入口资源
<img width="400" height="260" alt="image" src="https://github.com/user-attachments/assets/d3d67881-683e-48c7-9c0a-f62b3904af74" />


* --type: 后续接中间件类型，目前仅支持上述5种类型
* --order: 后续接指定中间件业务入口的资源编号, 多个编号间用英文逗号分隔；不是必须参数，当没有此参数时表示选择指定type的所有入口资源


#### pe(profiling event)
此命令的作用是选择要采集事件(分析资源)类型，有cpu,alloc(内存),wall(耗时),lock(锁竞争), 对应async-profiler的4种火焰图类型
参数列表:
* -l: list, 列举出当前应用支持的事件类型, 很多应用不支持alloc，具体要看jdk版本和操作系统类型
* -s: select, 选择要采集的事件类型
* --cpu/alloc/wall/lock: 后续接采集间隔，alloc是内存分配，单位是kb,其他3个单位是ms。可同时选择多个事件，支持同时采集多种事件生成对应火焰图。此参数的意思是指定采集间隔,具体含义请了解async-profiler采样原理。
* -c: clear, 清除已选中的事件类型


#### pf(profiling)
此命令的作用是操作async-profiler，开始/结束profiling,或者查询状态
* start: 启动profiling。启动后，需要触发选择的业务入口请求，如果30秒内没有触发，会启动失败，可重复启动。如果选择业务入口时只指定type，则触发指定type的任一请求即可。
* stop:  结束profiling。
* status: 查询profiling状态，有未运行和运行了多长两种状态。
* --duration: 设置profiling持续时间，时间到后自动结束，未到时间也可以通过stop命令结束，非必须参数。
* --file: profiling结束后生成火焰图文件的名称，默认生成在运行woody工具的目录，如果同时采集多种事件，会添加事件类型前缀以区分。如果文件名称不是.html后缀，会追加文件后缀。如果此参数未指定, 则会缓存采样结果，供后续ts命令操作。

#### ts(trace sample)



