#io.spider
######spider使用java语言开发，使用Spring作为IoC容器，采用TCP/IP协议，在此基础上，结合SaaS金融交易系统的特性进行针对性和重点设计，以更加灵活和高效的满足金融交易系统多租户、高可用、分布式部署的要求。spider默认采用JSON作为序列化机制，后续版本可能会考虑支持protobuf（java/c++/c#均有类库支持）。	为了最大化性能以及稳定性，spider基于Oracle JDK1.8进行编译并应避免使用deprecated特性。	为了尽可能的适应各环境以及互联网应用，spider应能至少运行于tomcat/jboss应用服务器或原生java下。
######spider设计和参考文档请参考https://wenku.baidu.com/view/99b5fc06a31614791711cc7931b765ce05087af1