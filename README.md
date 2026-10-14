# payledger

纯 Java 的复式记账核心库：记账过账、账户余额、对账、多边净额清结算、Webhook 验签，以及多租户上下文隔离。核心层不依赖 Spring，可在任意 JVM 程序中使用；提供 Spring Boot 自动装配层与示例应用。

## 模块

| 模块 | 说明 |
| --- | --- |
| `payledger-core` | 零 Spring 核心：金额（`Money`）、账户（`Account`）、分录（`LedgerEntry`）、事务（`Transaction`）、过账（`PostingService`）、余额与分录存储、对账（`Reconciler`）、清结算（`SettlementEngine`）、Webhook 验签（`WebhookVerifier`）、支付渠道（`PaymentChannel`）与多租户上下文（`PayledgerTenant`）。仅依赖 JDK 21。 |
| `payledger-starter` | Spring Boot 自动装配：按 `X-Tenant-Id` 请求头解析租户，提供 `LedgerController`（`POST /payledger/transactions`、`GET /payledger/balances/{accountId}`）与内存实现 Bean，可按配置启用微信 / 支付宝 / Apple 支付渠道。 |
| `payledger-sample` | 示例 Web 应用：启动即演示开户、过账、查余额、对账与清结算，并自带 MockMvc 测试。 |

## 快速开始

```yaml
payledger:
  default-currency: CNY            # 默认币种，缺省 CNY
  tenant-header: X-Tenant-Id      # 租户头，缺省 X-Tenant-Id
  webhook:
    enabled: true
    channels:
      wechat: demo-secret          # 渠道 ID -> 验签密钥
```

```java
// 纯核心用法，不依赖 Spring
PayledgerTenant.runWith("demo", () -> {
    Account cash = new Account("acc_cash", "demo", AccountType.ASSET, "CNY", AccountStatus.OPEN);
    Account revenue = new Account("acc_rev", "demo", AccountType.INCOME, "CNY", AccountStatus.OPEN);

    EntryStore entries = new InMemoryEntryStore();
    BalanceStore balances = new InMemoryBalanceStore();
    balances.openAccount(cash);
    balances.openAccount(revenue);

    PostingService posting = new PostingService(entries, balances);
    Transaction tx = new Transaction("tx1", "demo", Instant.now(), "收款");
    tx.addEntry(new LedgerEntry("e1", "demo", "acc_cash", Direction.DEBIT, Money.of("100.00", "CNY"), "tx1", "REF1", "现金增加"));
    tx.addEntry(new LedgerEntry("e2", "demo", "acc_rev", Direction.CREDIT, Money.of("100.00", "CNY"), "tx1", "REF1", "收入增加"));
    posting.post(tx);

    Money balance = balances.getBalance("demo", "acc_cash"); // 100.00 CNY
});
```

## 核心能力

- **复式记账与过账**：`Transaction` 必须借贷平衡，否则 `TransactionValidator` 抛出 `UnbalancedEntryException`；`PostingService.post` 在提交前逐项校验账户存在、未冻结、币种一致。
- **余额**：`InMemoryBalanceStore` 以租户 + 账户为键维护余额，过账时原子地写入分录与更新余额。
- **对账**：`Reconciler.reconcile` 按参考号与金额绝对值匹配内部分录与外部对账单（`ExternalStatement`），产出匹配项、未匹配项与差异项。
- **清结算**：`SettlementEngine.net` 对多边转账做净额轧差，得到各方净应收 / 应付指令（`SettlementInstruction`）。
- **Webhook 验签**：`WebhookVerifier` 使用 HMAC-SHA256 对请求体签名并做常量时间比对，签名大小写不敏感且自动去除首尾空格。

## 构建

```bash
./mvnw -B verify
```

`verify` 会执行编译、单元测试、集成测试与 JaCoCo 覆盖率门禁（整体 >= 0.60，core >= 0.75）。

## 架构约束

`payledger-core` 严禁依赖 `org.springframework.*`（CI 的 `architecture-boundaries` job 强制扫描）。
