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

## 运行项目
### 1. 使用 Maven 运行
在项目根目录下执行以下命令：
```sh
mvn spring-boot:run
```
## API 说明
创建交易
- URL：/api/transactions
- 方法：POST
- 请求体：
```json
{
    "description": "Test Transaction",
    "amount": 100.0
}
```
获取交易
- URL：/api/transactions/{id}
- 方法：GET

获取所有交易
- URL：/api/transactions
- 方法：GET

删除交易
- URL：/api/transactions/{id}
- 方法：DELETE

更新交易
- URL：/api/transactions/{id}
- 方法：PUT
- 请求体：
```json
{
    "description": "Updated Transaction",
    "amount": 200.0
}
```