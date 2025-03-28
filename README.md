# 银行交易管理应用程序

这是一个简易的银行交易管理应用程序，使用 Java 21 和 Spring Boot 开发。

## 项目结构
- `src/main/java`：包含 Java 源代码
- `src/test/java`：包含测试代码
- `pom.xml`：Maven 项目配置文件
- `Dockerfile`：用于容器化应用程序

## 依赖说明
- `spring-boot-starter-web`：用于构建 Web 应用程序
- `spring-boot-starter-test`：用于测试 Spring Boot 应用程序
- `junit-jupiter`：JUnit 5 测试框架
- `mockito-core`：用于模拟对象的测试框架
- `lombok`: 用于简化类的构造函数声明
- `spring-data-commons`: 用于page类，分页参数
- `spring-boot-starter-validation`: 用于校验的声明
- `spring-boot-starter-cache`: 用于cache


## 运行项目
### 1. 使用 Maven 运行
在项目根目录下执行以下命令：
```sh
mvn spring-boot:run
```
## API 说明
创建交易(用于指定用户创建交易)
- URL：/api/v1/users/${userName}/transactions
- 方法：POST
- 请求体：
```json
{
  "userName": "testUser",
  "amount": 100.00,
  "type": "DEPOSIT",
  "description": "测试存款"
}
```
获取交易(用于获取指定用户的某个交易)
- URL：/api/v1/users/${userName}/transactions/{id}
- 方法：GET

获取所有交易(获取指定用户的部份分页交易)
- URL：/api/v1/users/${userName}/transactions?page=${page}&size=${size}
- 方法：GET

删除交易
- URL：/api/v1/users/${userName}/transactions/{id}
- 方法：DELETE

更新交易
- URL：/api/v1/users/${userName}/transactions/{id}
- 方法：PUT
- 请求体：
```json
{
  "userName": "testUser",
  "amount": 9999.11,
  "type": "DEPOSIT",
  "description": "改为存款"
}
```