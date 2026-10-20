# payledger

纯 Java 的复式记账核心库：记账过账、账户余额、对账、多边净额清结算、Webhook 验签，以及多租户上下文隔离。核心层不依赖 Spring，可在任意 JVM 程序中使用；提供 Spring Boot 自动装配层与示例应用。

## 模块

| 模块 | 职责 |
| --- | --- |
| `payledger-contract` | 跨模块共享的**领域契约**：不可变值对象（`Money` / `Account` / `LedgerEntry` / `Transaction` / 外部流水等）、枚举（`AccountStatus` / `AccountType` / `Direction` / `TransactionStatus` / `SignatureAlgorithm`）、领域异常与纯逻辑校验器（`TransactionValidator`）。零 Spring、仅依赖 JDK 21，是所有模块的依赖底座。 |
| `payledger-store` | 存储 **SPI 与参考实现**：余额 / 分录 / 幂等去重三类存储接口（`BalanceStore` / `EntryStore` / `IdempotencyStore`）及其线程安全的内存实现（`InMemory*`）。依赖 `payledger-contract`，是替换真实存储（数据库 / Redis）的唯一接入点。 |
| `payledger-core` | 业务**编排层**：过账（`PostingService`）、对账（`Reconciler`）、清结算（`SettlementEngine`）、Webhook 验签（`WebhookVerifier`）、支付渠道（`PaymentChannel` 及微信 / 支付宝 / Apple 实现）与多租户上下文（`PayledgerTenant`）。依赖 contract 与 store，自身仍严禁 Spring。 |
| `payledger-starter` | Spring Boot **自动装配**：按 `X-Tenant-Id` 请求头解析租户，提供 `LedgerController`（`POST /payledger/transactions`、`GET /payledger/balances/{accountId}`）与内存实现 Bean，可按配置启用微信 / 支付宝 / Apple 支付渠道。 |
| `payledger-sample` | 示例 Web 应用：启动即演示开户、过账、查余额、对账与清结算，并自带 MockMvc 测试。 |

> 拆分原则：全限定类名（`io.github.nephets.payledger.core.*`）保持不变，仅物理目录在模块间迁移，对外 API 零破坏性。

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

`verify` 会执行编译、单元测试、集成测试与 JaCoCo 覆盖率门禁（整体 >= 0.60，`payledger-core` >= 0.75，其余模块 >= 0.60）。

## 设计决策

本节记录关键设计背后的「为什么」，便于后续维护者理解取舍。

1. **为什么拆出 `contract` / `store` 两个独立模块？** 领域值对象、枚举、异常与存储 SPI 是所有上层能力的公共依赖。把它们从 `core` 抽离成 `payledger-contract` 与 `payledger-store`，使核心编排层只依赖稳定、无副作用的契约；未来要接入数据库 / Redis 存储时，只需新增一个 `store` 实现，不必改动核心逻辑，也避免核心层膨胀。

2. **为什么 `Money` 固定 2 位小数且强制币种隔离？** 金融金额绝不能用二进制浮点直接参与运算（误差会累积成错账）。`Money` 构造时即四舍五入到 2 位，且只有同币种的两个金额才能加减，币种不一致立即抛 `CurrencyMismatchException`，从根上杜绝「美元 + 人民币」混算。

3. **为什么 `PostingService.post` 先「预演」再提交？** 提交前逐一校验账户存在、未冻结、币种一致，并算出应有余额；全部通过后才统一写余额与分录。任一校验失败都不落账，保证原子性，避免出现「半笔账」这种资损级别的不一致。

4. **为什么余额更新用 `AtomicReference` + CAS 而非 `volatile Money`？** 并发过账对同一账户是 read-modify-write。`Money` 是不可变值对象，`accumulateAndGet` 在 CAS 保护下完成「读旧 → 算新 → 写回」，保证高并发下不丢更新（早期实现用非原子的 volatile RMW，高并发会丢更新）。

5. **为什么 `InMemory*` 存储都有容量上限与 FIFO 淘汰？** 内存存储若无界增长，长期运行必致 OOM。三个实现都按「开户 / 写入顺序」在超出容量时淘汰最旧记录，使内存占用有上界；同时保留取号器（`nextSequenceNo`）与幂等去重。

6. **为什么 `IdempotencyStore.consume` 必须原子（恰好一次）？** 支付回调可能重复投递，去重是防重复入账的最后一道闸。默认实现的 `seen` 与 `mark` 非原子，并发下多个线程可能都先看到「未处理」再都标记，从而都返回 `true`；`InMemoryIdempotencyStore` 在锁内完成「判断 + 标记」，仅真正首次写入的线程返回 `true`，保证恰好一次。

7. **为什么多租户用 `InheritableThreadLocal` 且必须配对恢复？** 所有账户余额与分录都按「当前线程租户」隔离。`PayledgerTenant.runWith` / `with` 用 try-finally 恢复原上下文，防止线程池串租户导致 A 的数据写进 B 的账（资损级 bug）。

8. **为什么对账按「参考号 + 金额绝对值」匹配？** 参考号是业务单号，金额取绝对值可兼容借贷方向差异；单号相同且金额相等进「匹配」，单号相同但金额对不上进「差异」，单号完全对不上进「未匹配内部 / 外部」，三类分开便于告警与人工复核。

9. **为什么 Webhook 验签用常量时间比较？** 验签比较若按内容提前返回，会被时序侧信道利用推测密钥 / 签名；`constantTimeEquals` 对两串逐字节异或后统一判定，且签名大小写不敏感、自动去首尾空格，以兼容各渠道报文格式。

## 架构约束

`payledger-contract` 与 `payledger-core` 严禁依赖 `org.springframework.*`（CI 的 `architecture-boundaries` job 强制扫描）。
